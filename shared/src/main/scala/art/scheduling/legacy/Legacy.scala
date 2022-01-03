// #Sireum

package art.scheduling.legacy

import org.sireum._
import art.{Art, Bridge}
import art.scheduling.Scheduler

@record class Legacy(bridges: ISZ[art.Bridge]) extends Scheduler {

  override def initialize(): Unit = { }

  override def initializationPhase(): Unit = {
    for (bridge <- bridges) {
      bridge.entryPoints.initialise()
      Art.logInfo(bridge.id, s"Initialized bridge: ${bridge.name}")
    }
  }

  override def computePhase(): Unit = {
    LegacyExtensions.computePhase(bridges)
  }

  override def finalizePhase(): Unit = {
    for (bridge <- bridges) {
      bridge.entryPoints.finalise()
      Art.logInfo(bridge.id, s"Finalized bridge: ${bridge.name}")
    }
  }
}

@ext object LegacyExtensions {
  def computePhase(bridges: ISZ[art.Bridge]): Unit = $
}
