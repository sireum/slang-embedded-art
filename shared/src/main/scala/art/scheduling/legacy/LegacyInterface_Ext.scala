package art.scheduling.legacy

import art.{Art, ArtNative, ArtNative_Ext, DispatchPropertyProtocol}
import scala.collection.mutable.{Map => MMap}
import org.sireum._

object LegacyInterface_Ext {
  val slowdown: Z = 1

  /** The arguments to Thread.sleep(millis, nanos) for a sleep of rate * slowdown nanoseconds;
    * computed in Z, so neither the product nor a sub-millisecond rate is lost */
  def sleepArgs(rate: Art.Time, slowdown: Z): (scala.Long, scala.Int) = {
    val ns = rate.toMP * slowdown.toMP
    return ((ns / 1000000).toLong, (ns % 1000000).toInt)
  }

  def computePhase(bridges: IS[Art.BridgeId, art.Bridge]): Unit = {
    var terminated = false
    var numTerminated: Z = 0

    for (bridge <- bridges) {

      val (rate, isSporadic) = bridge.dispatchProtocol match {
        case DispatchPropertyProtocol.Periodic(period) => (period, F)
        case DispatchPropertyProtocol.Sporadic(min) => (min, T)
      }

      new Thread(() => {
        ArtNative.logInfo(Art.logTitle, s"Thread for ${bridge.name} instantiated.")
        ArtNative_Ext.synchronized {
          ArtNative_Ext.wait()
        }
        while (!terminated) {
          val (sleepMs, sleepNs) = sleepArgs(rate, slowdown)
          Thread.sleep(sleepMs, sleepNs)
          if (ArtNative.shouldDispatch(bridge.id)) {
            try {
              bridge.synchronized {
                bridge.entryPoints.compute()
              }
            }
            catch {
              case x: Throwable =>
                x.printStackTrace()
                terminated = true
            }
          }
        }
        ArtNative_Ext.synchronized {
          numTerminated += 1
        }
      }).start()
    }

    Thread.sleep(1000)

    ArtNative.logInfo(Art.logTitle, s"Start execution (press Enter twice to terminate) ...")

    ArtNative_Ext.synchronized {
      ArtNative_Ext.notifyAll()
    }

    Console.in.readLine()
    terminated = true

    while (numTerminated != bridges.size) {
      Thread.sleep(1000)
    }
  }

  def concMap[K, V](): MMap[K, V] = {
    import org.sireum.$internal.CollectionCompat.Converters._
    new java.util.concurrent.ConcurrentHashMap[K, V].asInstanceOf[java.util.Map[K, V]].asScala
  }
}
