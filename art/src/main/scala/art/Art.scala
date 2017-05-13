// #Sireum

package art

import org.sireum._

object Art {
  type PortId = Z
  type BridgeId = Z

  val maxComponents: PortId = 3 // constant set during instantiation, must be < Z32.Max
  val maxPorts: PortId = 7 // constant set during instantiation, must be < Z32.Max

  val logTitle: String = "Art"
  val bridges: MS[BridgeId, Option[Bridge]] = MS.create[BridgeId, Option[Bridge]](maxComponents, None[Bridge]())
  val connections: MS[PortId, PortId] = MS.create[PortId, PortId](maxPorts, Z32.toZ(Z32.Max))

  def bridge(bridgeId: BridgeId): Bridge = {
    val Some(r) = bridges(bridgeId)
    return r
  }

  def register(bridge: Bridge): Unit = {
    bridges(bridge.id) = Some(bridge)
    bridge.dispatchProtocol match {
      case DispatchPropertyProtocol.Periodic(period) =>
        ArtNative.logInfo(logTitle, s"Registered component: ${bridge.name} (periodic: $period)")
      case DispatchPropertyProtocol.Sporadic(min) =>
        ArtNative.logInfo(logTitle, s"Registered component: ${bridge.name} (sporadic: $min)")
    }
    for (port <- bridge.ports.all) {
      port.mode match {
        case PortMode.DataIn => ArtNative.logInfo(logTitle, s"- Registered port: ${port.name} (data in)")
        case PortMode.DataOut => ArtNative.logInfo(logTitle, s"- Registered port: ${port.name} (data out)")
        case PortMode.EventIn => ArtNative.logInfo(logTitle, s"- Registered port: ${port.name} (event in)")
        case PortMode.EventOut => ArtNative.logInfo(logTitle, s"- Registered port: ${port.name} (event out)")
      }
    }
  }

  // can't find definition in the standard ??
  def dispatchStatus(bridgeId: Art.BridgeId): DispatchStatus = { // DISPATCH_STATUS
    return ArtNative.dispatchStatus(bridgeId)
  }

  def receiveInput(eventPortIds: ISZ[PortId], dataPortIds: ISZ[PortId]): Unit = { // RECEIVE_INPUT
    ArtNative.receiveInput(eventPortIds, dataPortIds)
  }

  def putValue(portId: PortId, data: DataContent): Unit = { // PUT_VALUE
    ArtNative.putValue(portId, data)
  }

  def getValue(portId: PortId): DataContent = { // GET_VALUE
    val r = ArtNative.getValue(portId)
    return r
  }

  def sendOutput(eventPortIds: ISZ[Art.PortId], dataPortIds: ISZ[Art.PortId]): Unit = { // SEND_OUTPUT
    ArtNative.sendOutput(eventPortIds, dataPortIds)
  }

  def logInfo(bridgeId: Art.BridgeId, msg: String): Unit = {
    ArtNative.logInfo(bridge(bridgeId).name, msg)
  }

  def logError(bridgeId: Art.BridgeId, msg: String): Unit = {
    ArtNative.logError(bridge(bridgeId).name, msg)
  }

  def logDebug(bridgeId: Art.BridgeId, msg: String): Unit = {
    ArtNative.logDebug(bridge(bridgeId).name, msg)
  }

  def connect(from: UPort, to: UPort): Unit = {
    connections(from.id) = to.id
    ArtNative.logInfo(logTitle, s"Connected ports: ${from.name} -> ${to.name}")
  }

  def run(system: ArchitectureDescription): Unit = {

    for (component <- system.components) {
      register(component)
    }

    for (connection <- system.connections) {
      connect(connection.from, connection.to)
    }

    ArtNative.run()
  }
}
