package art.scheduling.roundrobin

import org.sireum._
import org.sireum.$internal.###

trait RoundRobinInterface {
  def init(): Unit

  def loop(roundRobin: RoundRobin): Unit
}

object RoundRobinExtensions_Ext {

  var roundRobinInterface: RoundRobinInterface = _

  def init(): Unit = {
    roundRobinInterface.init()
  }

  def loop(roundRobin: RoundRobin): Unit = {
    roundRobinInterface.loop(roundRobin)
  }
}
