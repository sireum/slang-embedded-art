package art

import org.sireum.{ISZ, String}
import art.Art.Time

import scala.collection.mutable.{Map => MMap}
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

 //{ISZ, String, sig}

object TimerApi {
  type EventId = String
}

import TimerApi._

trait TimerApi {
  val m: MMap[String, AtomicBoolean] = ArtNative_Ext.concMap()
  val executor = Executors.newSingleThreadScheduledExecutor()

  def syncObject : Object

  def dataOutPortIds: ISZ[Art.PortId]

  def eventOutPortIds: ISZ[Art.PortId]

  def init(e: String): EventId

  def finalise(): Unit = {
    executor.shutdownNow()
  }

  def event(e: EventId): Unit = {
    m.get(e) match {
      case Some(b) =>
        b.set(false)
        m.remove(e)
      case _ =>
    }
  }

  def timeout(e: EventId, n: Time, p: () => Unit): Unit = {
    assert(m.get(e).isEmpty)
    var b = new AtomicBoolean(true)

    //def handler(): Unit = {
    //  if (b.get()) p()
    //}

    class c() extends Runnable {
      def run(): Unit = {
        if (b.get()) {
          syncObject.synchronized {
            // FIXME: should in data ports be fetched?
            p()
            Art.sendOutput(eventOutPortIds, dataOutPortIds)
          }
        }
      }
    }

    m.put(e, b)

    val adjusted = n.toMP.toLong * ArtNative_Ext.slowdown.toMP.toLong

    //Executor.timeout(n, handler _)
    executor.schedule(new c(), adjusted, TimeUnit.MILLISECONDS)
  }
}
