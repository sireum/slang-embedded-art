package aadl.runtime

import AADL.Bridge.DispatchPropertyProtocol._
import org.sireum._

import scala.collection.immutable.ListSet

object AADLExt_Ext {
  val slowdown = z64"100"
  var rates: ListSet[Z64] = ListSet()
  var rateBridges: Map[Z64, ISZ[AADL.BridgeId]] = Map()
  val eventPortVariables: scala.collection.mutable.Map[AADL.PortId, (Z64, Any)] = concMap()
  val dataPortVariables: scala.collection.mutable.Map[AADL.PortId, (Z64, Any)] = concMap()
  val receivedPortValues: scala.collection.mutable.Map[AADL.PortId, Any] = concMap()
  val sentPortValues: scala.collection.mutable.Map[AADL.PortId, Any] = concMap()

  def dispatchStatus(bridgeId: AADL.BridgeId): Option[AADL.PortId] = {
    val is = AADL.bridges(bridgeId).inPortIds.elements.filter(eventPortVariables.contains)
    is.sortBy(p => eventPortVariables(p)._1.value).headOption match {
      case scala.Some(in) => Some(in)
      case _ => None()
    }
  }

  def receiveInput(eventPortIdOpt: Option[AADL.PortId], dataPortIds: ISZ[AADL.PortId]): Unit = {
    for (portId <- eventPortIdOpt) {
      val v = eventPortVariables(portId)._2
      eventPortVariables -= portId
      receivedPortValues(portId) = v
    }
    for (portId <- dataPortIds) {
      receivedPortValues(portId) = dataPortVariables(portId)._2
    }
  }

  def putValue[T](portId: AADL.PortId, data: T): Unit = {
    sentPortValues(portId) = data
  }

  def getValue[T](portId: AADL.PortId): T = {
    receivedPortValues(portId).asInstanceOf[T]
  }

  def sendOutput(eventPortIds: ISZ[AADL.PortId], dataPortIds: ISZ[AADL.PortId]): Unit = {
    val time = toZ64(System.currentTimeMillis())
    for (portId <- eventPortIds; v <- sentPortValues.get(portId)) {
      eventPortVariables(AADL.connections(portId)) = (time, v)
    }
    for (portId <- dataPortIds; v <- sentPortValues.get(portId)) {
      dataPortVariables(AADL.connections(portId)) = (time, v)
    }
    for (portId <- eventPortIds) {
      sentPortValues -= portId
    }
    for (portId <- dataPortIds) {
      sentPortValues -= portId
    }
  }

  def logInfo(bridgeId: AADL.BridgeId, msg: String): Unit = logInfo(AADL.bridges(bridgeId).name, msg)

  def logError(bridgeId: AADL.BridgeId, msg: String): Unit = logError(AADL.bridges(bridgeId).name, msg)

  def logDebug(bridgeId: AADL.BridgeId, msg: String): Unit = logDebug(AADL.bridges(bridgeId).name, msg)

  def logInfo(title: String, msg: String): Unit = log("info", title, msg)

  def logError(title: String, msg: String): Unit = log("error", title, msg)

  def logDebug(title: String, msg: String): Unit = log("debug", title, msg)

  def log(kind: String, title: String, msg: String): Unit = {
    Console.out.println(s"""{ "log" : "$kind", "title" : ${escape(title)}, "msg" : ${escape(msg)}, "time" : "${System.currentTimeMillis()}" }""")
    Console.out.flush()
  }

  def escape(raw: String): String = {
    import scala.reflect.runtime.universe._
    Literal(Constant(raw)).toString
  }

  def run(): Unit = {
    for (bridge <- AADL.bridges) bridge.dispatchProtocol match {
      case Periodic(period) =>
        val rate = N32.toZ64(period)
        rates += rate
        rateBridges += rate -> (rateBridges.getOrElse(rate, ISZ()) :+ bridge.id)
      case _ =>
    }

    require(rates.nonEmpty)

    for (bridge <- AADL.bridges) {
      bridge.dispatchProtocol match {
        case Periodic(_) =>
        case Sporadic(min) =>
          val minRate = N32.toZ64(min)
          val (less, more) = rates.toVector.sortBy(_.value).map(r => (r - minRate, r)).partition(_._1 >= z64"0")
          val rate = ((less.lastOption, more.headOption): @unchecked) match {
            case (_, scala.Some((_, r))) => r
            case (scala.Some((_, r)), _) => r
          }
          rateBridges += rate -> (rateBridges.getOrElse(rate, ISZ()) :+ bridge.id)
      }
      bridge.entryPoints.initialise()
      logInfo(AADL.logTitle, s"Initialized bridge: ${bridge.name}")
    }

    var terminated = false
    var numTerminated = 0
    var hyperPeriod: Z64 = slowdown
    for (rate <- rates) {
      hyperPeriod *= rate
      new Thread(() => {
        logInfo(AADL.logTitle, s"Thread for rate group $rate instantiated.")
        AADLExt_Ext.synchronized {
          AADLExt_Ext.wait()
        }
        while (!terminated) {
          Thread.sleep((rate * slowdown).value)
          var bridgesToCompute = List[AADL.BridgeId]()
          for (bridgeId <- rateBridges(rate)) {
            val bridge = AADL.bridges(bridgeId)
            bridge.dispatchProtocol match {
              case Periodic(_) => bridgesToCompute ::= bridgeId
              case Sporadic(min) =>
                val minRate = N32.toZ64(min).value
                val lastSporadic = AADL.lastSporadic(bridgeId).value
                if (System.currentTimeMillis() - lastSporadic < minRate) {
                  // skip
                } else if (bridge.inPortIds.elements.exists(eventPortVariables.contains)) {
                  bridgesToCompute ::= bridgeId
                  AADL.lastSporadic(bridgeId) = toZ64(System.currentTimeMillis())
                } else {
                  // skip
                }
            }
          }
          for (bridgeId <- bridgesToCompute.par)
            AADL.bridges(bridgeId).entryPoints.compute()
        }
        AADLExt_Ext.synchronized {
          numTerminated += 1
        }
      }).start()
    }

    Thread.sleep(hyperPeriod.value)

    logInfo(AADL.logTitle, s"Start execution...")
    AADLExt_Ext.synchronized {
      AADLExt_Ext.notifyAll()
    }

    Console.in.readLine()
    terminated = true

    while (numTerminated != rates.size) {
      Thread.sleep(hyperPeriod.value)
    }
    logInfo(AADL.logTitle, s"End execution...")

    for (bridge <- AADL.bridges) {
      bridge.entryPoints.finalise()
      logInfo(AADL.logTitle, s"Finalized bridge: ${bridge.name}")
    }
  }

  def toZ64(value: Long): Z64 = org.sireum.math._Z64(value)

  def concMap[K, V](): scala.collection.mutable.Map[K, V] = {
    import scala.collection.JavaConverters._
    new java.util.concurrent.ConcurrentHashMap[K, V].asScala
  }
}
