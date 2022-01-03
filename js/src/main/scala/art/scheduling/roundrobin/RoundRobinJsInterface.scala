package art.scheduling.roundrobin

class RoundRobinJsInterface() extends RoundRobinInterface {
  override def init(): Unit = {
    // do nothing
  }

  override def loop(roundRobin: RoundRobin): Unit = {
    roundRobin.hyperPeriod()
    scala.scalajs.js.timers.setTimeout(0) {
      loop(roundRobin)
    }
  }
}