package art

import org.sireum._
import org.sireum.S64._
import org.sireum.test.TestSuite
import art.scheduling.legacy.LegacyInterface_Ext
import art.scheduling.roundrobin.RoundRobin

/** ART's time handling (hamr-codegen doc/ExactTime-design.md, D6) */
class ArtTimeTests extends TestSuite {

  def pid(n: Int): Art.PortId = Art.PortId.fromZ(Z(n))

  def bid(n: Int): Art.BridgeId = Art.BridgeId.fromZ(Z(n))

  def eventIn(n: Int): UPort = Port[Empty](pid(n), s"p$n", PortMode.EventIn)

  def urgentEventIn(n: Int, urgency: Int): UPort = UrgentPort[Empty](pid(n), s"p$n", PortMode.EventIn, Z(urgency))

  def eventOut(n: Int): UPort = Port[Empty](pid(n), s"p$n", PortMode.EventOut)

  def register(id: Int, eventIns: ISZ[UPort], eventOuts: ISZ[UPort], dp: DispatchPropertyProtocol): Unit =
    Art.register(TestBridge(bid(id), s"b$id", Bridge.Ports(dataIns = ISZ(), dataOuts = ISZ(),
      eventIns = eventIns, eventOuts = eventOuts), TestEntryPoints(), dp))

  def portIds(ns: Int*): ISZ[Art.PortId] = ISZ(ns.map(pid): _*)

  val oneSecond: Art.Time = s64"1000000000"

  "ArtTime converts to nanoseconds in S64" in {
    assert(ArtTime.millis(500) == s64"500000000")
    assert(ArtTime.micros(100) == s64"100000")
    // over 2^31 ns, which a 32-bit Z could not hold
    assert(ArtTime.millis(3000) == s64"3000000000")
  }

  "legacy scheduler sleep arguments" in {
    assert(LegacyInterface_Ext.sleepArgs(s64"100000", 1) == ((0L, 100000)))
    assert(LegacyInterface_Ext.sleepArgs(s64"1000001", 1) == ((1L, 1)))
    assert(LegacyInterface_Ext.sleepArgs(s64"2500000000", 1) == ((2500L, 0)))
    assert(LegacyInterface_Ext.sleepArgs(s64"1500000", 2) == ((3L, 0)))
  }

  "the clock is non-negative and monotonic without Art.run" in {
    val t1 = Art.time()
    val t2 = Art.time()
    assert(t1 >= s64"0" && t2 >= t1)
  }

  "round robin dispatches every bridge on its first pass" in {
    ArtNative_Ext.inInfrastructurePorts.clear()
    register(0, ISZ(), ISZ(), DispatchPropertyProtocol.Periodic(oneSecond))
    register(1, ISZ(eventIn(1)), ISZ(), DispatchPropertyProtocol.Sporadic(oneSecond))
    val rr = RoundRobin(ISZ(bid(0), bid(1)))
    rr.backdateLastDispatches()
    assert(rr.shouldDispatch(bid(0)), "periodic")
    assert(!rr.shouldDispatch(bid(1)), "sporadic without an event")
    ArtNative_Ext.insertInInfrastructurePort(pid(1), Empty())
    assert(rr.shouldDispatch(bid(1)), "sporadic with its first event")
    ArtNative_Ext.inInfrastructurePorts.clear()
  }

  "JVM event ports of the same urgency are dispatched in arrival order" in {
    ArtNative_Ext.inInfrastructurePorts.clear()
    register(2, ISZ(eventIn(10), eventIn(11), eventIn(12)), ISZ(), DispatchPropertyProtocol.Sporadic(oneSecond))
    // inserted in the reverse of the declaration order, in the same clock tick or not
    for (n <- Seq(12, 10, 11)) ArtNative_Ext.insertInInfrastructurePort(pid(n), Empty())
    assert(ArtNative_Ext.dispatchStatus(bid(2)) == EventTriggered(portIds(12, 10, 11)))
    ArtNative_Ext.inInfrastructurePorts.clear()

    register(3, ISZ(urgentEventIn(13, 5), urgentEventIn(14, 5), urgentEventIn(15, 9)), ISZ(),
      DispatchPropertyProtocol.Sporadic(oneSecond))
    for (n <- Seq(14, 13, 15)) ArtNative_Ext.insertInInfrastructurePort(pid(n), Empty())
    assert(ArtNative_Ext.dispatchStatus(bid(3)) == EventTriggered(portIds(15, 14, 13)))
    ArtNative_Ext.inInfrastructurePorts.clear()
  }

  "Slang (transpiled) event ports of the same urgency are dispatched in arrival order" in {
    ArtNativeSlang.inInfrastructurePorts = Map.empty
    ArtNativeSlang.outPortVariables = Map.empty
    register(4, ISZ(), ISZ(eventOut(20), eventOut(21)), DispatchPropertyProtocol.Periodic(oneSecond))
    register(5, ISZ(eventIn(22), eventIn(23)), ISZ(), DispatchPropertyProtocol.Sporadic(oneSecond))
    // connected crosswise, so arrival order is the reverse of the consumer's declaration order
    Art.connect(eventOut(20), eventIn(23))
    Art.connect(eventOut(21), eventIn(22))
    ArtNativeSlang.putValue(pid(20), Empty())
    ArtNativeSlang.putValue(pid(21), Empty())
    ArtNativeSlang.sendOutput(portIds(20, 21), ISZ())
    assert(ArtNativeSlang.dispatchStatus(bid(5)) == EventTriggered(portIds(23, 22)))
    ArtNativeSlang.inInfrastructurePorts = Map.empty
  }
}
