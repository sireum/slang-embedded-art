// #Sireum
package art.scheduling.roundrobin

import org.sireum._
import org.sireum.S64._
import art.scheduling.Scheduler
import art.{Art, ArtNative, DispatchPropertyProtocol}

@record class RoundRobin(bridges: ISZ[art.Bridge]) extends Scheduler {

  var lastDispatch: MS[Art.BridgeId, Art.Time] = MS.create(bridges.size, s64"0")
  var lastSporadic: MS[Art.BridgeId, Art.Time] = MS.create(bridges.size, s64"0")

  override def initialize(): Unit = {
    RoundRobinExtensions.init()
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
        if(Art.time() - lastDispatch(bridge.id) > conversions.Z.toS64(period)) {
          return ArtNative.shouldDispatch(bridge.id)  // will always return true
        } else {
          return F
        }
      case DispatchPropertyProtocol.Sporadic(minRate) =>
        if(Art.time() - lastSporadic(bridge.id) < conversions.Z.toS64(minRate)) {
          return F
        } else {
          // check if there are events waiting in incoming infrastructure port
          return ArtNative.shouldDispatch(bridge.id)
        }
    }
  }

  def hyperPeriod(): Unit = {
    for (bridge <- bridges) {
      if (shouldDispatch(bridge)) {
        lastDispatch(bridge.id) = Art.time()
        bridge.entryPoints.compute()

        if (bridge.dispatchProtocol.isInstanceOf[DispatchPropertyProtocol.Sporadic]) {
          lastSporadic(bridge.id) = Art.time()
        }
      }
    }
  }

  override def computePhase(): Unit = {
    //while(!RoundRobinExtensions.shouldStop()) {
    //  hyperPeriod()
    //}

    // infinite looping may lock up the interface so instead have the
    // the platform impl figure out how to achieve that.

    // Transpiler doesn't support passing functions
    //RoundRobinExtensions.loop(hyperPeriod _)

    // invoke hyperPeriod once here so that the transpiler picks it up
    hyperPeriod()

    // now have the platform implementations call hyperPeriod via 'this'
    // using whatever loop mechanism they can implement
    RoundRobinExtensions.loop(this)
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

  def loop(roundRobin: RoundRobin): Unit = $
}