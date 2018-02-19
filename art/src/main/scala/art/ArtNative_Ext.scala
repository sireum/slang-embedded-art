package art

import org.sireum._

import scala.collection.mutable.{Map => MMap, Set => MSet}

object ArtNative_Ext {
  val noTime: Art.Time = 0

  val slowdown: Z = 100

  val lastSporadic: MMap[Art.BridgeId, Art.Time] = concMap()
  val eventPortVariables: MMap[Art.PortId, DataContent] = concMap()
  val dataPortVariables: MMap[Art.PortId, DataContent] = concMap()
  val receivedPortValues: MMap[Art.PortId, DataContent] = concMap()
  val sentPortValues: MMap[Art.PortId, DataContent] = concMap()
  val debugObjects: MMap[String, Any] = concMap()
  val portListeners: MMap[Art.PortId, MSet[DataContent => Unit]] = concMap()

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
          portListeners.get(portId).map(s => s.foreach(f => f(data)))
        case _ =>
      }
    }
    for (portId <- dataPortIds) {
      dataPortVariables.get(portId) match {
        case scala.Some(data) =>
          receivedPortValues(portId) = data
          portListeners.get(portId).map(s => s.foreach(f => f(data)))
        case _ =>
      }
    }
  }

  def putValue(portId: Art.PortId, data: DataContent): Unit = {
    sentPortValues(portId) = data
  }

  def getValue(portId: Art.PortId): Option[DataContent] = {
    val data = receivedPortValues.get(portId) match {
      case scala.Some(v) => org.sireum.Some(v)
      case _ => org.sireum.None[DataContent]()
    }
    return data
  }

  def sendOutput(eventPortIds: ISZ[Art.PortId], dataPortIds: ISZ[Art.PortId]): Unit = { // SEND_OUTPUT
    for (portId <- eventPortIds ++ dataPortIds) {
      sentPortValues.get(portId) match {
        case scala.Some(data) =>
          for(p <- Art.connections(portId).elements) {
            Art.port(p).mode match {
              case PortMode.DataIn | PortMode.DataOut =>
                dataPortVariables(p) = data
              case PortMode.EventIn | PortMode.EventOut =>
                eventPortVariables(p) = data
            }
          }
          portListeners.get(portId).map(s => s.foreach(f => f(data)))

          sentPortValues -= portId
        case _ =>
      }
    }
  }

  def setDebugObject[T](key: String, o: T): Unit = {
    logDebug(Art.logTitle, s"Set debug object for $key")
    debugObjects(key) = o
  }

  def getDebugObject[T](key: String): Option[T] = {
    debugObjects.get(key) match {
      case scala.Some(o) => Some(o.asInstanceOf[T])
      case _ => None[T]()
    }
  }

  def injectPort(bridgeId: Art.BridgeId, port: Art.PortId, data: DataContent): Unit = {
    assert(z"0" <= bridgeId && bridgeId < Art.maxComponents && Art.bridges(bridgeId).nonEmpty)

    val bridge = Art.bridges(bridgeId).get
    assert(bridge.ports.all.elements.map(_.id).contains(port))

    if(bridge.ports.dataOuts.elements.map(_.id).contains(port) ||
       bridge.ports.eventOuts.elements.map(_.id).contains(port)) {

      logDebug(Art.logTitle, s"Injecting from port ${Art.ports(port).get.name}")

      putValue(port, data)
      sendOutput(bridge.ports.eventOuts.map(_.id), bridge.ports.dataOuts.map(_.id))
    } else {
      logDebug(Art.logTitle, s"Injecting to port ${Art.ports(port).get.name}")

      if(bridge.ports.dataIns.elements.map(_.id).contains(port)) {
        dataPortVariables(port) = data
      } else {
        eventPortVariables(port) = data
      }
    }
  }

  def registerPortListener(portId: Art.PortId, callback: DataContent => Unit): Unit = {
    assert(z"-1" <= portId && portId < Art.maxPorts)
    assert(z"-1" == portId || Art.ports(portId).nonEmpty)

    val portIds: Seq[UPort] =
      if (portId == -1) {
        // Issues with allowing components to subscribe to everything:
        //   - will continuously invoke callback when dataIn ports for periodic components receive data.  Would cause
        //     an issue if action should only be taken when the data changes
        //   - related to above, client would probably want to know what port was triggered.  Could change callback to
        //     (UPort, DataContent) => Unit.  Client could maintain a map of UPort -> DataContent to detect when
        //     dataIn ports change
        Art.ports.elements.filter(_.nonEmpty).map(_.get)
      } else {
        Seq(Art.ports(portId).get)
      }

    for (p <- portIds) {
      val c = if (p.mode == PortMode.DataIn || p.mode == PortMode.EventIn) "receives" else "sends"
      val t = if (p.mode == PortMode.DataIn || p.mode == PortMode.DataOut) "data" else "event"
      logDebug(Art.logTitle, s"Registered callback.  Triggered when port ${p.name} $c $t")
      portListeners.getOrElseUpdate(p.id, concSet()).add(callback)
    }
  }

  def logInfo(title: String, msg: String): Unit = log("info", title, msg)

  def logError(title: String, msg: String): Unit = log("error", title, msg)

  def logDebug(title: String, msg: String): Unit = log("debug", title, msg)

  def time(): Art.Time = toZ(System.currentTimeMillis())

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
    //require(Art.bridges.elements.forall(_.nonEmpty))

    val bridges = {
      var r = Vector[Bridge]()
      for (e <- Art.bridges.elements) e match {
        case Some(b) => r :+= b
        case _ =>
      }
      r
    }

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
          if (shouldDispatch(bridge.id))
            try {
              bridge.synchronized {
                bridge.entryPoints.compute()
              }
            }
            catch {
              case x : Throwable =>
                x.printStackTrace()
                terminated = true
            }
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

    ArtTimer_Ext.finalise()
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

  def concSet[K](): MSet[K] = {
    import scala.collection.JavaConverters._
    val m: java.util.Set[K] = java.util.concurrent.ConcurrentHashMap.newKeySet()
    m.asScala
  }
}
