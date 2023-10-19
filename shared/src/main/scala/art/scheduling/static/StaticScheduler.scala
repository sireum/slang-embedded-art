// #Sireum
package art.scheduling.static

import org.sireum._
import art.Art
import art.scheduling.Scheduler
import art.scheduling.static.Schedule.DScheduleSpec
import isolette.Arch

object StaticScheduler {
  var threadNickNames: Map[String, Art.BridgeId] = Map.empty
  var domainToBridgeIdMap: ISZ[Art.BridgeId] = ISZ()

  def bridgeIdToDomainMap(bridgeId: Art.BridgeId): Z = {
    for (i <- 0 until domainToBridgeIdMap.size if bridgeId == domainToBridgeIdMap(i)) {
      return i
    }
    assert(F, s"domain for $bridgeId not found")
    halt(s"domain for $bridgeId not found")
  }
}

@record class StaticScheduler(staticSchedule: DScheduleSpec,
                              domainToBridgeIdMap: ISZ[Art.BridgeId],
                              threadNickNames: Map[String, Art.BridgeId],
                              commandProvider: CommandProvider) extends Scheduler {

  override def initialize(): Unit = {
    StaticScheduler.threadNickNames = threadNickNames
    StaticScheduler.domainToBridgeIdMap = domainToBridgeIdMap

    Schedule.setSchedule(staticSchedule, domainToBridgeIdMap)

    Explorer.initializeScheduleStateIMP()
  }

  override def initializationPhase(): Unit = {
    for (bridgeId <- domainToBridgeIdMap) {
      Arch.ad.components(bridgeId).entryPoints.initialise()
      art.Art.logInfo(bridgeId, s"Initialized bridge: ${Arch.ad.components(bridgeId).name}")
    }
  }

  override def computePhase(): Unit = {
    var done: B = F
    while (!done) {
      done = CommandInterpreter.interpretCmd(commandProvider.nextCommand())
    }
  }

  override def finalizePhase(): Unit = {
    for (bridgeId <- domainToBridgeIdMap) {
      Arch.ad.components(bridgeId).entryPoints.finalise()
      art.Art.logInfo(bridgeId, s"Finalized bridge: ${Arch.ad.components(bridgeId).name}")
    }
  }
}
