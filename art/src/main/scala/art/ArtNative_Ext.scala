package art

import org.sireum._

import scala.collection.mutable.{Map => MMap}

object ArtNative_Ext {
  type Time = Z

  val noTime: Time = 0

  val lastSporadic: MMap[Art.BridgeId, Time] = concMap()
  val eventPortVariables: MMap[Art.PortId, DataContent] = concMap()
  val dataPortVariables: MMap[Art.PortId, DataContent] = concMap()
  val receivedPortValues: MMap[Art.PortId, DataContent] = concMap()
  val sentPortValues: MMap[Art.PortId, DataContent] = concMap()

  def dispatchStatus(bridgeId: Art.BridgeId): DispatchStatus = {
    val portIds = ISZ[Art.PortId](Art.bridge(bridgeId).ports.eventIns.elements.map(_.id).filter(eventPortVariables.get(_).nonEmpty): _*)
    if (portIds.isEmpty) TimeTriggered() else EventTriggered(portIds)
  }

  def receiveInput(eventPortIds: ISZ[Art.PortId], dataPortIds: ISZ[Art.PortId]): Unit = {
    for (portId <- eventPortIds) {
      eventPortVariables.get(portId) match {
        case scala.Some(data) =>
          eventPortVariables -= portId
          receivedPortValues(portId) = data
        case _ =>
      }
    }
    for (portId <- dataPortIds) {
      dataPortVariables.get(portId) match {
        case scala.Some(data) =>
          receivedPortValues(portId) = data
        case _ =>
      }
    }
  }

  def putValue(portId: Art.PortId, data: DataContent): Unit = {
    sentPortValues(portId) = data
  }

  def getValue(portId: Art.PortId): DataContent = {
    val data = receivedPortValues(portId)
    return data
  }

  def sendOutput(eventPortIds: ISZ[Art.PortId], dataPortIds: ISZ[Art.PortId]): Unit = { // SEND_OUTPUT
    for (portId <- eventPortIds) {
      sentPortValues.get(portId) match {
        case scala.Some(data) =>
          eventPortVariables(Art.connections(portId)) = data
          sentPortValues -= portId
        case _ =>
      }
    }
    for (portId <- eventPortIds) {
      sentPortValues.get(portId) match {
        case scala.Some(data) =>
          dataPortVariables(Art.connections(portId)) = data
          sentPortValues -= portId
        case _ =>
      }
    }
  }

  def logInfo(title: String, msg: String): Unit = log("info", title, msg)

  def logError(title: String, msg: String): Unit = log("error", title, msg)

  def logDebug(title: String, msg: String): Unit = log("debug", title, msg)

  def time(): Time = toZ(System.currentTimeMillis())

  def shouldDispatch(bridgeId: Art.BridgeId): B = {
    val b = Art.bridge(bridgeId)
    b.dispatchProtocol match {
      case DispatchPropertyProtocol.Periodic(_) => return T
      case DispatchPropertyProtocol.Sporadic(minRate) =>
        val ls = lastSporadic.getOrElse(bridgeId, noTime)
        if (time() - ls < minRate) {
          return F
        } else {
          return b.ports.eventIns.elements.exists(port => eventPortVariables.contains(port.id))
        }
    }
  }

  def run(): Unit = {
    require(Art.bridges.elements.forall(_.nonEmpty))

    val bridges = Art.bridges.elements.map({ case Some(b) => b })

    val slowdown: Z = 100

    for (bridge <- bridges) {
      bridge.entryPoints.initialise()
      logInfo(Art.logTitle, s"Initialized bridge: ${bridge.name}")
    }

    var terminated = false
    var numTerminated = 0

    for (bridge <- bridges) {
      val rate = bridge.dispatchProtocol match {
        case DispatchPropertyProtocol.Periodic(period) => period
        case DispatchPropertyProtocol.Sporadic(min) => min
      }

      new Thread(() => {
        logInfo(Art.logTitle, s"Thread for ${bridge.name} instantiated.")
        ArtNative_Ext.synchronized {
          ArtNative_Ext.wait()
        }
        while (!terminated) {
          Thread.sleep((rate * slowdown).toMP.toLong)
          if (shouldDispatch(bridge.id)) bridge.entryPoints.compute()
        }
        ArtNative_Ext.synchronized {
          numTerminated += 1
        }
      }).start()
    }

    Thread.sleep(1000)

    logInfo(Art.logTitle, s"Start execution (press Enter twice to terminate) ...")

    ArtNative_Ext.synchronized {
      ArtNative_Ext.notifyAll()
    }

    Console.in.readLine()
    terminated = true

    while (numTerminated != bridges.size) {
      Thread.sleep(1000)
    }
    logInfo(Art.logTitle, s"End execution...")

    for (bridge <- bridges) {
      bridge.entryPoints.finalise()
      logInfo(Art.logTitle, s"Finalized bridge: ${bridge.name}")
    }
  }

  def log(kind: String, title: String, msg: String): Unit = {
    Console.out.println(s"""{ "log" : "$kind", "title" : ${escape(title)}, "msg" : ${escape(msg)}, "time" : "${time()}" }""")
    Console.out.flush()
  }

  def escape(raw: String): String = {
    import scala.reflect.runtime.universe._
    Literal(Constant(raw.value)).toString
  }

  def toZ(value: Long): Z = Z(value)

  def concMap[K, V](): MMap[K, V] = {
    import scala.collection.JavaConverters._
    new java.util.concurrent.ConcurrentHashMap[K, V].asScala
  }
}
