// #Sireum

package art.scheduling.roundrobin.legacy

import org.sireum._
import art.Art
import art.scheduling.Scheduler

@record class RoundRobin(bridges: MSZ[art.Bridge]) extends Scheduler {

  override def initialize(): Unit = {}

  override def initializationPhase(): Unit = {
    for (bridge <- bridges) {
      bridge.entryPoints.initialise()
      Art.logInfo(bridge.id, s"Initialized bridge: ${bridge.name}")
    }
  }

  override def computePhase(): Unit = {
    LegacyRoundRobin.computePhase(bridges)
  }

  override def finalizePhase(): Unit = {
    for (bridge <- bridges) {
      bridge.entryPoints.finalise()
      Art.logInfo(bridge.id, s"Finalized bridge: ${bridge.name}")
    }
  }
}

@ext object LegacyRoundRobin {
  def computePhase(bridges: MSZ[art.Bridge]): Unit = $
}
