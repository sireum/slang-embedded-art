package art

import org.sireum._

import scala.collection.immutable.ListSet

object ArtNative_Ext {
  val slowdown = z64"100"
  var rates: ListSet[Z64] = ListSet()
  var rateBridges: Map[Z64, ISZ[Art.BridgeId]] = Map()
  val eventPortVariables: scala.collection.mutable.Map[Art.PortId, (Z64, Any)] = concMap()
  val dataPortVariables: scala.collection.mutable.Map[Art.PortId, (Z64, Any)] = concMap()
  val receivedPortValues: scala.collection.mutable.Map[Art.PortId, Any] = concMap()
  val sentPortValues: scala.collection.mutable.Map[Art.PortId, Any] = concMap()

  def dispatchStatus(bridgeId: Art.BridgeId): Option[Art.PortId] = {
    val is = Art.bridges(bridgeId).ports.mode(PortMode.EventIn).elements.map(_.id).filter(eventPortVariables.contains)
    is.sortBy(p => eventPortVariables(p)._1.value).headOption match {
      case scala.Some(in) => Some(in)
      case _ => None()
    }
  }

  def receiveInput(eventPortIdOpt: Option[Art.PortId], dataPortIds: ISZ[Art.PortId]): Unit = {
    for (portId <- eventPortIdOpt) {
      val v = eventPortVariables(portId)._2
      eventPortVariables -= portId
      receivedPortValues(portId) = v
    }
    for (portId <- dataPortIds) {
      receivedPortValues(portId) = dataPortVariables(portId)._2
    }
  }

  def putValue[T](portId: Art.PortId, data: T): Unit = {
    sentPortValues(portId) = data
  }

  def getValue[T](portId: Art.PortId): T = {
    receivedPortValues(portId).asInstanceOf[T]
  }

  def sendOutput(eventPortIds: ISZ[Art.PortId], dataPortIds: ISZ[Art.PortId]): Unit = {
    val time = toZ64(System.currentTimeMillis())
    for (portId <- eventPortIds; v <- sentPortValues.get(portId)) {
      eventPortVariables(Art.connections(portId)) = (time, v)
    }
    for (portId <- dataPortIds; v <- sentPortValues.get(portId)) {
      dataPortVariables(Art.connections(portId)) = (time, v)
    }
    for (portId <- eventPortIds) {
      sentPortValues -= portId
    }
    for (portId <- dataPortIds) {
      sentPortValues -= portId
    }
  }

  def logInfo(bridgeId: Art.BridgeId, msg: String): Unit = logInfo(Art.bridges(bridgeId).name, msg)

  def logError(bridgeId: Art.BridgeId, msg: String): Unit = logError(Art.bridges(bridgeId).name, msg)

  def logDebug(bridgeId: Art.BridgeId, msg: String): Unit = logDebug(Art.bridges(bridgeId).name, msg)

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
    for (bridge <- Art.bridges) bridge.dispatchProtocol match {
      case DispatchPropertyProtocol.Periodic(period) =>
        val rate = N32.toZ64(period)
        rates += rate
        rateBridges += rate -> (rateBridges.getOrElse(rate, ISZ()) :+ bridge.id)
      case _ =>
    }

    require(rates.nonEmpty)

    for (bridge <- Art.bridges) {
      bridge.dispatchProtocol match {
        case DispatchPropertyProtocol.Periodic(_) =>
        case DispatchPropertyProtocol.Sporadic(min) =>
          val minRate = N32.toZ64(min)
          val (less, more) = rates.toVector.sortBy(_.value).map(r => (r - minRate, r)).partition(_._1 >= z64"0")
          val rate = ((less.lastOption, more.headOption): @unchecked) match {
            case (_, scala.Some((_, r))) => r
            case (scala.Some((_, r)), _) => r
          }
          rateBridges += rate -> (rateBridges.getOrElse(rate, ISZ()) :+ bridge.id)
      }
      bridge.entryPoints.initialise()
      logInfo(Art.logTitle, s"Initialized bridge: ${bridge.name}")
    }

    var terminated = false
    var numTerminated = 0
    var hyperPeriod = slowdown
    for (rate <- rates) {
      hyperPeriod *= rate
      new Thread(() => {
        logInfo(Art.logTitle, s"Thread for rate group $rate instantiated.")
        ArtNative_Ext.synchronized {
          ArtNative_Ext.wait()
        }
        while (!terminated) {
          Thread.sleep((rate * slowdown).value)
          var bridgesToCompute = List[Art.BridgeId]()
          for (bridgeId <- rateBridges(rate)) {
            val bridge = Art.bridges(bridgeId)
            bridge.dispatchProtocol match {
              case DispatchPropertyProtocol.Periodic(_) => bridgesToCompute ::= bridgeId
              case DispatchPropertyProtocol.Sporadic(min) =>
                val minRate = N32.toZ64(min).value
                val lastSporadic = Art.lastSporadic(bridgeId).value
                if (System.currentTimeMillis() - lastSporadic < minRate) {
                  // skip
                } else if (bridge.ports.mode(PortMode.EventIn).elements.map(_.id).exists(eventPortVariables.contains)) {
                  bridgesToCompute ::= bridgeId
                  Art.lastSporadic(bridgeId) = toZ64(System.currentTimeMillis())
                } else {
                  // skip
                }
            }
          }
          for (bridgeId <- bridgesToCompute.par)
            Art.bridges(bridgeId).entryPoints.compute()
        }
        ArtNative_Ext.synchronized {
          numTerminated += 1
        }
      }).start()
    }

    Thread.sleep(hyperPeriod.value)

    logInfo(Art.logTitle, s"Start execution...")
    ArtNative_Ext.synchronized {
      ArtNative_Ext.notifyAll()
    }

    Console.in.readLine()
    terminated = true

    while (numTerminated != rates.size) {
      Thread.sleep(hyperPeriod.value)
    }
    logInfo(Art.logTitle, s"End execution...")

    for (bridge <- Art.bridges) {
      bridge.entryPoints.finalise()
      logInfo(Art.logTitle, s"Finalized bridge: ${bridge.name}")
    }
  }

  def toZ64(value: Long): Z64 = org.sireum.math._Z64(value)

  def concMap[K, V](): scala.collection.mutable.Map[K, V] = {
    import scala.collection.JavaConverters._
    new java.util.concurrent.ConcurrentHashMap[K, V].asScala
  }
}
