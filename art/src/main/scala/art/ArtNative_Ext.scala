package art

import org.sireum._

object ArtNative_Ext {
  def logInfo(title: String, msg: String): Unit = log("info", title, msg)

  def logError(title: String, msg: String): Unit = log("error", title, msg)

  def logDebug(title: String, msg: String): Unit = log("debug", title, msg)

  def time(): Art.Time = toZ64(System.currentTimeMillis())

  def run(): Unit = {
    require(Art.bridges.elements.forall(_.nonEmpty))

    val bridges = Art.bridges.elements.map({ case Some(b) => b })

    import scala.collection.immutable.ListSet

    val slowdown = z64"100"
    var rates: ListSet[Z64] = ListSet()
    var rateBridges: Map[Z64, ISZ[Art.BridgeId]] = Map()

    for (bridge <- bridges) bridge.dispatchProtocol match {
      case DispatchPropertyProtocol.Periodic(period) =>
        val rate = N32.toZ64(period)
        rates += rate
        rateBridges += rate -> (rateBridges.getOrElse(rate, ISZ()) :+ bridge.id)
      case _ =>
    }

    require(rates.nonEmpty)

    for (bridge <- bridges) {
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
          for (bridgeId <- rateBridges(rate).elements.filter(Art.shouldDispatch))
            Art.bridge(bridgeId).entryPoints.compute()
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
    Literal(Constant(raw)).toString
  }

  def toZ64(value: Long): Z64 = org.sireum.math._Z64(value)
}
