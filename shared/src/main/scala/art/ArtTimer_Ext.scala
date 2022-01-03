package art

import art.Art.BridgeId
import org.sireum.S64._
import org.sireum.{B, String, Z}

import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.{Executors, TimeUnit}
import scala.collection.mutable.{Map => MMap}

object ArtTimer_Ext {

  val noTime: Art.Time = s64"0"

  val slowdown: Z = 1

  val m: MMap[String, AtomicBoolean] = concMap()
  val executor = Executors.newSingleThreadScheduledExecutor()

  def finalise(): Unit = {
    executor.shutdownNow()
    ArtNative.logInfo(Art.logTitle, "Finalized ArtTimer")
  }

  def clearTimeout(eventId: String): Unit = {
    m.get(eventId) match {
      case Some(b) =>
        b.set(false)
        m.remove(eventId)
        ArtNative.logInfo(Art.logTitle, s"timeout cleared for $eventId")
      case _ =>
    }
  }

  def setTimeout(bridgeId: BridgeId, eventId: String, wait: Art.Time, autoClear: B, callback: () => Unit): Unit = {
    if(m.get(eventId).nonEmpty) {
      art.Art.logError(bridgeId, s"callback already set for $eventId")
      return
    }

    if(wait < s64"0") {
      art.Art.logError(bridgeId, s"Invalid wait time: ${wait}.  Value must be non-negative.")
      return
    }

    var b = new AtomicBoolean(true)

    val bridge = Art.bridges(bridgeId).get
    val eventOuts = bridge.ports.eventOuts.map(_.id)
    val dataOuts = bridge.ports.dataOuts.map(_.id)

    val task = new Runnable {
      override def run(): Unit = {
        if (b.get()) {
          bridge.synchronized {
            callback()
            Art.sendOutput(eventOuts, dataOuts)
            if(autoClear) {
              clearTimeout(eventId)
            }
          }
        }
      }
    }

    m.put(eventId, b)

    val adjusted = wait.toMP.toLong * slowdown.toMP.toLong
    executor.schedule(task, adjusted, TimeUnit.MILLISECONDS)

    art.Art.logInfo(bridgeId, s"callback set for $eventId")
  }

  def concMap[K, V](): MMap[K, V] = {
    import org.sireum.$internal.CollectionCompat.Converters._
    new java.util.concurrent.ConcurrentHashMap[K, V].asInstanceOf[java.util.Map[K, V]].asScala
  }
}