// #Sireum

package art

import org.sireum._

object Art {
  type PortId = Z
  type BridgeId = Z
  type Time = Z

  val maxComponents: PortId = 29 // constant set during instantiation, must be < Z32.Max
  val maxPorts: PortId = 1024 // constant set during instantiation, must be < Z32.Max

  val logTitle: String = "Art"
  val bridges: MS[BridgeId, Option[Bridge]] = MS.create[BridgeId, Option[Bridge]](maxComponents, None[Bridge]())
  val connections: MS[PortId, ISZ[PortId]] = MS.create[PortId, ISZ[PortId]](maxPorts, ISZ())
  val ports: MS[PortId, Option[UPort]] = MS.create[PortId, Option[UPort]](maxPorts, None[UPort]())

  def bridge(bridgeId: BridgeId): Bridge = {
    val Some(r) = bridges(bridgeId)
    return r
  }

  def port(p: PortId) : UPort = {
    val Some(r) = ports(p)
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
      ports(port.id) = Some(port)
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

  def getValue(portId: PortId): Option[DataContent] = { // GET_VALUE
    ArtNative.getValue(portId)
  }

  def sendOutput(eventPortIds: ISZ[Art.PortId], dataPortIds: ISZ[Art.PortId]): Unit = { // SEND_OUTPUT
    ArtNative.sendOutput(eventPortIds, dataPortIds)
  }

  def registerPortListener(portId: Art.PortId, callback: DataContent => Unit): Unit = {
    ArtNative.registerPortListener(portId, callback)
  }

  def injectPort(bridgeId: Art.BridgeId, portId: Art.PortId, d: DataContent): Unit = {
    ArtNative.injectPort(bridgeId, portId, d)
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
    connections(from.id) = connections(from.id) :+ to.id
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

  def time(): Time = {
    return ArtNative.time()
  }
}
