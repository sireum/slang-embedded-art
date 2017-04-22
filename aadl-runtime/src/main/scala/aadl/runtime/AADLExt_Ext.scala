package aadl.runtime

import AADL.Bridge.DispatchPropertyProtocol._
import org.sireum._

import scala.collection.immutable.ListSet

object AADLExt_Ext {
  val slowdown = 100
  var bridges: Vector[AADL.Bridge] = Vector()
  var connections: Map[AADL.PortId, AADL.PortId] = Map()
  var bridgeInPorts: Map[AADL.Bridge, Vector[AADL.PortId]] = Map()
  var rates: ListSet[Long] = ListSet()
  var rateBridges: Map[Long, Vector[AADL.Bridge]] = Map()
  var eventPortVariables: Map[AADL.PortId, (Long, Any)] = Map()
  var dataPortVariables: Map[AADL.PortId, Any] = Map()
  var lastSporadic: Map[AADL.Bridge, Long] = Map()
  var receivedPortValues: Map[AADL.PortId, Any] = Map()
  var sentPortValues: Map[AADL.PortId, Any] = Map()

  def dispatchStatus(bridge: AADL.Bridge): Option[AADL.PortId] = {
    bridgeInPorts.get(bridge) match {
      case scala.Some(ins) =>
        val is = ins.filter(eventPortVariables.contains)
        is.sortBy(p => eventPortVariables(p)._1).headOption match {
          case scala.Some(in) => Some(in)
          case _ => None()
        }
      case _ => None()
    }
  }

  def receiveInput(portIds: ISZ[AADL.PortId]): Unit = {
    for (portId <- portIds) {
      eventPortVariables.get(portId) match {
        case scala.Some((_, v)) =>
          eventPortVariables -= portId
          receivedPortValues += portId -> v
        case _ =>
          dataPortVariables.get(portId) match {
            case scala.Some(v) =>
              receivedPortValues += portId -> v
            case _ => assert(false)
          }
      }
    }
  }

  def putValue[T](portId: AADL.PortId, data: T): Unit = {
    sentPortValues += portId -> data
  }

  def getValue[T](portId: AADL.PortId): T = receivedPortValues(portId).asInstanceOf[T]

  def sendOutput(): Unit = {
    val time = System.currentTimeMillis()
    for ((portId, v) <- sentPortValues) {
      eventPortVariables += connections(portId) -> (time, v)
    }
    receivedPortValues = Map()
    sentPortValues = Map()
  }

  def registerBridge(bridge: AADL.Bridge): Unit = {
    bridges :+= bridge
    bridgeInPorts += bridge -> bridge.inPortIds.elements.toVector
    val protocol = bridge.dispatchProtocol match {
      case Periodic(period) =>
        val rate = N32.toZ64(period).value
        rates += rate
        rateBridges += rate -> (rateBridges.getOrElse(rate, Vector()) :+ bridge)
        s"(periodic: $period)"
      case Sporadic(min) =>
        s"(sporadic: $min)"
    }
    logInfo(AADL.logTitle, s"Registered bridge: ${bridge.name} $protocol")
  }

  def connect(from: AADL.PortId, to: AADL.PortId): Unit = {
    connections += from -> to
  }

  def logInfo(bridge: AADL.Bridge, msg: String): Unit = logInfo(bridge.name, msg)

  def logError(bridge: AADL.Bridge, msg: String): Unit = logError(bridge.name, msg)

  def logDebug(bridge: AADL.Bridge, msg: String): Unit = logDebug(bridge.name, msg)

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
    require(rates.nonEmpty)

    for (bridge <- bridges) {
      bridge.dispatchProtocol match {
        case Periodic(_) =>
        case Sporadic(min) =>
          val minRate = N32.toZ64(min).value
          val (less, more) = rates.toVector.sorted.map(r => (r - minRate, r)).partition(_._1 >= 0)
          val rate = ((less.lastOption, more.headOption): @unchecked) match {
            case (_, scala.Some((_, r))) => r
            case (scala.Some((_, r)), _) => r
          }
          rateBridges += rate -> (rateBridges.getOrElse(rate, Vector()) :+ bridge)
      }
      bridge.entryPoints.initialise()
      logInfo(AADL.logTitle, s"Initialized bridge: ${bridge.name}")
    }

    var terminated = false
    var numTerminated = 0
    var hyperPeriod: Long = slowdown
    for (rate <- rates) {
      hyperPeriod *= rate
      new Thread(() => {
        logInfo(AADL.logTitle, s"Thread for rate group $rate instantiated.")
        AADLExt_Ext.synchronized {
          AADLExt_Ext.wait()
        }
        while (!terminated) {
          Thread.sleep(rate * slowdown)
          var bridgesToCompute = Vector[AADL.Bridge]()
          for (bridge <- rateBridges(rate)) {
            bridge.dispatchProtocol match {
              case Periodic(_) => bridgesToCompute +:= bridge
              case Sporadic(min) =>
                val minRate = N32.toZ64(min).value
                lastSporadic.get(bridge) match {
                  case scala.Some(x) if System.currentTimeMillis() - x < minRate => // skip
                  case _ if bridgeInPorts.get(bridge).exists(_.exists(eventPortVariables.contains)) =>
                    bridgesToCompute +:= bridge
                    lastSporadic += bridge -> System.currentTimeMillis()
                  case _ => // skip
                }
            }
          }
          AADLExt_Ext.synchronized {
            for (bridge <- bridgesToCompute) bridge.entryPoints.compute()
          }
        }
        AADLExt_Ext.synchronized {
          numTerminated += 1
        }
      }).start()
    }

    Thread.sleep(hyperPeriod)

    logInfo(AADL.logTitle, s"Start execution...")
    AADLExt_Ext.synchronized {
      AADLExt_Ext.notifyAll()
    }

    Console.in.readLine()
    terminated = true

    while (numTerminated != rates.size) {
      Thread.sleep(hyperPeriod)
    }
    logInfo(AADL.logTitle, s"End execution...")

    for (bridge <- bridges) {
      bridge.entryPoints.finalise()
      logInfo(AADL.logTitle, s"Finalized bridge: ${bridge.name}")
    }
  }
}
