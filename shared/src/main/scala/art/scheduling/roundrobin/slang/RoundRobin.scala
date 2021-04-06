// #Sireum
package art.scheduling.roundrobin.slang

import org.sireum._
import art.scheduling.Scheduler
import art.{Art, ArtNative, DispatchPropertyProtocol}

@record class RoundRobin(bridges: MSZ[art.Bridge]) extends Scheduler {

  var lastDispatch: MS[Art.BridgeId, Art.Time] = MS.create(bridges.size, 0)
  var lastSporadic: MS[Art.BridgeId, Art.Time] = MS.create(bridges.size, 0)

  override def initialize(): Unit = {
    // do nothing
  }

  override def initializationPhase(): Unit = {
    for (bridge <- bridges) {
      bridge.entryPoints.initialise()
      Art.logInfo(bridge.id, s"Initialized bridge: ${bridge.name}")
    }
  }

  def shouldDispatch(bridge: art.Bridge): B = {
    bridge.dispatchProtocol match {
      case DispatchPropertyProtocol.Periodic(period) =>
        if(Art.time() - lastDispatch(bridge.id) > period) {
          return ArtNative.shouldDispatch(bridge.id)  // will always return true
        } else {
          return F
        }
      case DispatchPropertyProtocol.Sporadic(minRate) =>
        if(Art.time() - lastSporadic(bridge.id) < minRate) {
          return F
        } else {
          // check if there are events waiting in incoming infrastructure port
          return ArtNative.shouldDispatch(bridge.id)
        }
    }
  }

  override def computePhase(): Unit = {
    RoundRobinExtensions.init()

    while(!RoundRobinExtensions.shouldStop()) {
      for (bridge <- bridges) {
        if(shouldDispatch(bridge)) {
          lastDispatch(bridge.id) = Art.time()
          bridge.entryPoints.compute()

          if(bridge.dispatchProtocol.isInstanceOf[DispatchPropertyProtocol.Sporadic]) {
            lastSporadic(bridge.id) = Art.time()
          }
        }
      }
    }
  }

  override def finalizePhase(): Unit = {
    for (bridge <- bridges) {
      bridge.entryPoints.finalise()
      Art.logInfo(bridge.id, s"Finalized bridge: ${bridge.name}")
    }
  }
}

@ext object RoundRobinExtensions {
  def init(): Unit = $
  def shouldStop(): B = $
}