package art.scheduling.roundrobin

import art.{Art, ArtNative}
import java.util.concurrent.atomic.AtomicBoolean

class RoundRobinJvmInterface extends RoundRobinInterface {
  var terminated = new AtomicBoolean(false)

  def init(): Unit = {
    ArtNative.logInfo(Art.logTitle, s"Start execution (press Enter twice to terminate) ...")

    new Thread(() => {
      Console.in.readLine()
      terminated.set(true)
    }).start()
  }

  def loop(roundRobin: RoundRobin): Unit = {
    while(!terminated.get()) {
      roundRobin.hyperPeriod()
    }
  }
}
