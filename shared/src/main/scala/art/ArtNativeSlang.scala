// #Sireum

package art

import org.sireum._
import org.sireum.S64._
import art.DispatchPropertyProtocol.{Periodic, Sporadic}
import art.scheduling.Scheduler

/** A Slang/ScalaJS compliant ArtNative interface suitable for transpiling
 * to Linux/C and Javascript.  Note that it is not thread safe so it should
 * not be used with, for example, the legacy scheduler
 *
 * Using the companion object as the transpiler forwards calls to ArtNative
 * to ArtNativeSlang singleton when using Slang based schedulers
 */
object ArtNativeSlang {

  var inInfrastructurePorts: Map[Art.PortId, ArtSlangMessage] = Map.empty
  var outInfrastructurePorts: Map[Art.PortId, ArtSlangMessage] = Map.empty
  var inPortVariables: Map[Art.PortId, ArtSlangMessage] = Map.empty
  var outPortVariables: Map[Art.PortId, ArtSlangMessage] = Map.empty

  def shouldDispatch(bridgeId: Art.BridgeId): B = {
    assert(Art.bridges(bridgeId).nonEmpty, s"Bridge ${bridgeId} does not exist")

    Art.bridges(bridgeId).get.dispatchProtocol match {
      case DispatchPropertyProtocol.Periodic(_) => return T
      case DispatchPropertyProtocol.Sporadic(minRate) =>

        val eventIns = Art.bridges(bridgeId).get.ports.eventIns

        var hasEvents = F
        // transpiler workaround -- doesn't support .exists
        for (e <- eventIns) {
          if (inInfrastructurePorts.contains(e.id)) {
            hasEvents = T
          }
        }
        return hasEvents
    }
  }

  def lt(a: art.UPort, b: art.UPort): B = { // reverse sort
    val r: B = (a, b) match {
      // sorting function to make prioritized sequence of event port ids
      //   compare p1 to p2  (p1 represents the port to process earlier, i.e., should have priority)
      case (p1: UrgentPortProto, p2: UrgentPortProto) =>
        // if p1 has a strictly less urgency it comes after p2
        if (p1.urgency < p2.urgency) F
        // if p1 has a strictly greater urgency, it comes before p2
        else if (p1.urgency > p2.urgency) T
        // if p1 and p2 have the same urgency, the ordering is determined by arrival timestamps
        else inInfrastructurePorts.get(p1.id).get.dstArrivalTimestamp < inInfrastructurePorts.get(p2.id).get.dstArrivalTimestamp
      case (_: UrgentPortProto, _: PortProto) => T // urgent ports take precedence
      case (_: PortProto, _: UrgentPortProto) => F // urgent ports take precedence
      case (p1: PortProto, p2: PortProto) =>
        inInfrastructurePorts.get(p1.id).get.dstArrivalTimestamp < inInfrastructurePorts.get(p2.id).get.dstArrivalTimestamp
    }
    return r
  }

  def sort(ports: ISZ[UPort]): ISZ[UPort] = {
    def insert(p: UPort, sorted: ISZ[UPort]): ISZ[UPort] = {
      if (sorted.isEmpty) {
        return ISZ(p)
      }
      else {
        if (lt(sorted(0), p)) {
          return sorted(0) +: insert(p, ops.ISZOps(sorted).tail)
        }
        else {
          return p +: sorted
        }
      }
    }

    if (ports.isEmpty) {
      return ports
    }
    else {
      val sorted = sort(ops.ISZOps(ports).tail)
      return insert(ports(0), sorted)
    }
  }

  def dispatchStatus(bridgeId: Art.BridgeId): DispatchStatus = {
    val ret: DispatchStatus = Art.bridges(bridgeId).get.dispatchProtocol match {
      case Periodic(_) => TimeTriggered()
      case Sporadic(_) =>
        // get ids for non-empty input event ports
        val portIds: ISZ[Art.PortId] =
          for (p <- Art.bridges(bridgeId).get.ports.eventIns if inInfrastructurePorts.get(p.id).nonEmpty) yield p.id

        if (portIds.isEmpty) {
          halt(s"Unexpected: shouldDispatch() should have returned true in order to get here, however the incoming event ports are empty for bridge id ${bridgeId}")
        }

        val urgentFifo = sort(for (p <- portIds) yield Art.port(p))
        EventTriggered(for (p <- urgentFifo) yield p.id)
    }
    return ret
  }

  def receiveInput(eventPortIds: ISZ[Art.PortId], dataPortIds: ISZ[Art.PortId]): Unit = {
    for (portId <- eventPortIds) {
      inInfrastructurePorts.get(portId) match {
        case Some(data) =>
          inInfrastructurePorts = inInfrastructurePorts - ((portId, data))
          inPortVariables = inPortVariables + (portId ~> data(receiveInputTimestamp = Art.time()))
        case _ =>
      }
    }
    for (portId <- dataPortIds) {
      inInfrastructurePorts.get(portId) match {
        case Some(data) =>
          inPortVariables = inPortVariables + (portId ~> data)
        case _ =>
      }
    }
  }

  def putValue(portId: Art.PortId, data: DataContent): Unit = {
    // wrap the Art.DataContent value into an ArtMessage with time stamps
    outPortVariables = outPortVariables + (portId ~>
      ArtSlangMessage(data = data, srcPortId = portId, putValueTimestamp = Art.time(),
        dstPortId = ArtSlangMessage.UNSET_PORT, sendOutputTimestamp = ArtSlangMessage.UNSET_TIME, dstArrivalTimestamp = ArtSlangMessage.UNSET_TIME, receiveInputTimestamp = ArtSlangMessage.UNSET_TIME))
  }

  def getValue(portId: Art.PortId): Option[DataContent] = {
    // To return the value of the port to the application code, project
    // out the actual payload value (v.data) from ArtMessage (which includes timestamps, etc.)
    // to Art.DataContent (the "top"/union data type supported by Art.
    // The projecting preserves the option of structure of ArtMessage value.
    if (inPortVariables.contains(portId)) {
      return Some(inPortVariables.get(portId).get.data)
    } else {
      return None()
    }
  }

  def sendOutput(eventPortIds: ISZ[Art.PortId], dataPortIds: ISZ[Art.PortId]): Unit = {
    for (srcPortId <- eventPortIds ++ dataPortIds) {
      outPortVariables.get(srcPortId) match {
        case Some(msg) => {

          // move payload from out port port variables to the out infrastructure ports
          outInfrastructurePorts = outInfrastructurePorts + (srcPortId ~> msg)
          outPortVariables = outPortVariables - ((srcPortId, msg))

          // simulate sending msg via transport middleware
          for (dstPortId <- Art.connections(srcPortId)) {
            val _msg = msg(dstPortId = dstPortId, sendOutputTimestamp = Art.time())

            // send via middleware

            inInfrastructurePorts = inInfrastructurePorts + (dstPortId ~>
              _msg(dstArrivalTimestamp = Art.time()))
          }

          // payload delivered so remove it from out infrastructure port
          outInfrastructurePorts = outInfrastructurePorts - ((srcPortId, msg))
        }
        case _ =>
      }
    }
    // could clear outPortVariables for passed in portids but not strictly necessary
  }


  def logInfo(title: String, msg: String): Unit = {
    println(s"${title}: ${msg}")
  }

  def logDebug(title: String, msg: String): Unit = {
    eprintln(s"${title}: ${msg}")
  }

  def logError(title: String, msg: String): Unit = {
    println(s"${title}: ${msg}")
  }


  def setUpSystemState(): Unit = {
    // probably nothing to do here
  }

  def tearDownSystemState(): Unit = {
    // probably nothing to do here
  }


  def initializePhase(): Unit = {
    // probably nothing to do here
  }

  def computePhase(): Unit = {
    // probably nothing to do here
  }

  def finalizePhase(): Unit = {
    // probably nothing to do here
  }


  def time(): Art.Time = {
    return Process.time()
  }

  def initSystemTest(scheduler: Scheduler): Unit = {
    halt("stub")
  }

  def finalizeSystemTest(): Unit = {
    halt("stub")
  }

  def initTest(bridge: Bridge): Unit = {
    halt("stub")
  }

  def executeTest(bridge: Bridge): Unit = {
    halt("stub")
  }

  def finalizeTest(bridge: Bridge): Unit = {
    halt("stub")
  }

  def releaseOutput(eventPortIds: ISZ[Art.PortId], dataPortIds: ISZ[Art.PortId]): Unit = {
    halt("stub")
  }

  def manuallyClearOutput(): Unit = {
    halt("stub")
  }

  def insertInPortValue(dstPortId: Art.PortId, data: DataContent): Unit = {
    halt("stub")
  }

  def observeOutPortValue(portId: Art.PortId): Option[DataContent] = {
    halt("stub")
  }

  def observeInPortValue(portId: Art.PortId): Option[DataContent] = {
    halt("stub")
  }

  def observeOutPortVariable(portId: Art.PortId): Option[DataContent] = {
    halt("stub")
  }
}


@record class ArtNativeSlang extends ArtNativeInterface {

  def shouldDispatch(bridgeId: Art.BridgeId): B = {
    return ArtNativeSlang.shouldDispatch(bridgeId)
  }

  def dispatchStatus(bridgeId: Art.BridgeId): DispatchStatus = {
    return ArtNativeSlang.dispatchStatus(bridgeId)
  }

  def receiveInput(eventPortIds: ISZ[Art.PortId], dataPortIds: ISZ[Art.PortId]): Unit = {
    ArtNativeSlang.receiveInput(eventPortIds, dataPortIds)
  }

  def putValue(portId: Art.PortId, data: DataContent): Unit = {
    ArtNativeSlang.putValue(portId, data)
  }

  def getValue(portId: Art.PortId): Option[DataContent] = {
    return ArtNativeSlang.getValue(portId)
  }

  def sendOutput(eventPortIds: ISZ[Art.PortId], dataPortIds: ISZ[Art.PortId]): Unit = {
    ArtNativeSlang.sendOutput(eventPortIds, dataPortIds)
  }

  def logInfo(title: String, msg: String): Unit = {
    ArtNativeSlang.logInfo(title, msg)
  }

  def logDebug(title: String, msg: String): Unit = {
    ArtNativeSlang.logDebug(title, msg)
  }

  def logError(title: String, msg: String): Unit = {
    ArtNativeSlang.logError(title, msg)
  }


  def setUpSystemState(): Unit = {
    ArtNativeSlang.setUpSystemState()
  }

  def tearDownSystemState(): Unit = {
    ArtNativeSlang.tearDownSystemState()
  }

  def initializePhase(): Unit = {
    ArtNativeSlang.initializePhase()
  }

  def computePhase(): Unit = {
    ArtNativeSlang.computePhase()
  }

  def finalizePhase(): Unit = {
    ArtNativeSlang.finalizePhase()
  }

  def time(): Art.Time = {
    return ArtNativeSlang.time()
  }

  def initSystemTest(scheduler: Scheduler): Unit = {
    ArtNativeSlang.initSystemTest(scheduler)
  }

  def finalizeSystemTest(): Unit = {
    ArtNativeSlang.finalizeSystemTest()
  }

  def initTest(bridge: Bridge): Unit = {
    ArtNativeSlang.initTest(bridge)
  }

  def executeTest(bridge: Bridge): Unit = {
    ArtNativeSlang.executeTest(bridge)
  }

  def finalizeTest(bridge: Bridge): Unit = {
    ArtNativeSlang.finalizeTest(bridge)
  }

  def releaseOutput(eventPortIds: ISZ[Art.PortId], dataPortIds: ISZ[Art.PortId]): Unit = {
    ArtNativeSlang.releaseOutput(eventPortIds, dataPortIds)
  }

  def manuallyClearOutput(): Unit = {
    ArtNativeSlang.manuallyClearOutput()
  }

  def insertInPortValue(dstPortId: Art.PortId, data: DataContent): Unit = {
    ArtNativeSlang.insertInPortValue(dstPortId, data)
  }

  def observeOutPortValue(portId: Art.PortId): Option[DataContent] = {
    return ArtNativeSlang.observeOutPortValue(portId)
  }

  def observeInPortValue(portId: Art.PortId): Option[DataContent] = {
    return ArtNativeSlang.observeInPortValue(portId)
  }

  def observeOutPortVariable(portId: Art.PortId): Option[DataContent] = {
    return ArtNativeSlang.observeOutPortVariable(portId)
  }
}

@ext object Process {
  def time(): Art.Time = $
}


object ArtSlangMessage {
  val UNSET_PORT: Art.PortId = -1
  val UNSET_TIME: Art.Time = s64"-1"
}

@datatype class ArtSlangMessage(data: DataContent,

                                srcPortId: Art.PortId,
                                dstPortId: Art.PortId,

                                // when putValue was called by producer
                                putValueTimestamp: Art.Time,

                                // when sendOutput transferred message from out port var of producer
                                sendOutputTimestamp: Art.Time,

                                // when message arrived via transport layer
                                dstArrivalTimestamp: Art.Time,

                                // when receiveInput transferred message to in port vars of consumer
                                receiveInputTimestamp: Art.Time
                               )

