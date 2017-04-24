// #Sireum

package art

import org.sireum._

object Art {
  type PortId = N32
  type BridgeId = N32

  val logTitle: String = "Art"
  val initIdVal: BridgeId  = n32"0"
  var bridgeIds: BridgeId = initIdVal
  var portIds: PortId = initIdVal
  var maxPorts: PortId = initIdVal
  var maxComponents: BridgeId = initIdVal
  var bridges: MS[BridgeId, Bridge] = MS[BridgeId, Bridge]()
  var connections: MS[PortId, PortId] = MS[PortId, PortId]()
  var lastSporadic: MS[Art.BridgeId, Z64] = MS[Art.BridgeId, Z64]()

  def initialise(numOfComponents: Z, numOfPorts: Z): Unit = {
    portIds = initIdVal
    bridgeIds = initIdVal
    maxComponents = Z.toN32(numOfComponents)
    maxPorts = Z.toN32(numOfPorts)
    bridges = MS[BridgeId, Bridge]()
    lastSporadic = MS.create[BridgeId, Z64](maxComponents, z64"0")
    connections = MS.create[PortId, PortId](maxPorts, initIdVal)
  }

  def freshPortId(name: String): PortId = {
    l"""{ requires portIds < maxPorts }"""
    val r = portIds
    portIds = r + n32"1"
    ArtNative.logInfo(logTitle, s"Created port id $r for $name")
    return r
  }

  def freshBridgeId(name: String): BridgeId = {
    l"""{ requires bridgeIds < maxBridges }"""
    val r = bridgeIds
    bridgeIds = r + n32"1"
    ArtNative.logInfo(logTitle, s"Created bridge id $r for $name")
    return r
  }

  def register(bridge: Bridge): Unit = {
    l"""{ requires bridgeIds < maxBridges }"""
    bridges = bridges :+ bridge
    bridge.dispatchProtocol match {
      case DispatchPropertyProtocol.Periodic(period) =>
        ArtNative.logInfo(logTitle, s"Registered bridge: ${bridge.name} (periodic: $period)")
      case DispatchPropertyProtocol.Sporadic(min) =>
        ArtNative.logInfo(logTitle, s"Registered bridge: ${bridge.name} (sporadic: $min)")
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
  def dispatchStatus(bridgeId: Art.BridgeId): Option[PortId] = { // DISPATCH_STATUS
    val r = ArtNative.dispatchStatus(bridgeId)
    return r
  }

  def receiveInput(eventPortIdOpt: Option[PortId], dataPortIds: ISZ[PortId]): Unit = { // RECEIVE_INPUT
    ArtNative.receiveInput(eventPortIdOpt, dataPortIds)
  }

  def putValue[T](portId: PortId, data: T): Unit = { // PUT_VALUE
    // record all port ids for sendOutput
    ArtNative.putValue[T](portId, data)
  }

  def getValue[T](portId: PortId): T = { // GET_VALUE
    val r = ArtNative.getValue[T](portId)
    return r
  }

  def sendOutput(eventPortIds: ISZ[Art.PortId], dataPortIds: ISZ[Art.PortId]): Unit = { // SEND_OUTPUT
    ArtNative.sendOutput(eventPortIds, dataPortIds)
  }

  def connect(from: Port, to: Port): Unit = {
    connections(from.id) = to.id
    ArtNative.logInfo(logTitle, s"Connected ports: ${from.name} -> ${to.name}")
  }
}

@ext object ArtNative {
  def dispatchStatus(bridgeId: Art.BridgeId): Option[Art.PortId] = $

  def receiveInput(eventPortIdOpt: Option[Art.PortId], dataPortIds: ISZ[Art.PortId]): Unit = $

  def putValue[T](portId: Art.PortId, data: T): Unit = $

  def getValue[T](portId: Art.PortId): T = $

  def sendOutput(eventPortIds: ISZ[Art.PortId], dataPortIds: ISZ[Art.PortId]): Unit = $

  def logInfo(title: String, msg: String): Unit = $

  def logError(title: String, msg: String): Unit = $

  def logDebug(title: String, msg: String): Unit = $

  def logInfo(bridgeId: Art.BridgeId, msg: String): Unit = $

  def logError(bridgeId: Art.BridgeId, msg: String): Unit = $

  def logDebug(bridgeId: Art.BridgeId, msg: String): Unit = $

  def run(): Unit = $
}
