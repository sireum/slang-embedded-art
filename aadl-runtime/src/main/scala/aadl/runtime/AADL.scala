// #Sireum

package aadl.runtime

import org.sireum._

object AADL {
  type PortId = N32
  type BridgeId = N32

  val logTitle = "AADL Runtime"
  val initIdVal = n32"0"
  var bridgeIds: BridgeId = initIdVal
  var portIds: PortId = initIdVal
  var maxPorts: PortId = initIdVal
  var maxBridges: BridgeId = initIdVal
  var bridges: MS[BridgeId, Bridge] = MS[BridgeId, Bridge]()
  var connections: MS[PortId, PortId] = MS[PortId, PortId]()
  var lastSporadic: MS[AADL.BridgeId, Z64] = MS[AADL.BridgeId, Z64]()

  def initialise(numOfBridges: Z, numOfPorts: Z): Unit = {
    portIds = initIdVal
    bridgeIds = initIdVal
    maxBridges = Z.toN32(numOfBridges)
    maxPorts = Z.toN32(numOfPorts)
    bridges = MS[BridgeId, Bridge]()
    lastSporadic = MS.create[BridgeId, Z64](maxBridges, z64"0")
    connections = MS.create[PortId, PortId](maxPorts, initIdVal)
  }

  def freshPortId(name: String): PortId = {
    l"""{ requires portIds < maxPorts }"""
    val r = portIds
    portIds = r + n32"1"
    AADLNative.logInfo(logTitle, s"Created port id $r for $name")
    return r
  }

  def freshBridgeId(name: String): BridgeId = {
    l"""{ requires bridgeIds < maxBridges }"""
    val r = bridgeIds
    bridgeIds = r + n32"1"
    AADLNative.logInfo(logTitle, s"Created bridge id $r for $name")
    return r
  }

  def registerBridge(bridge: Bridge): Unit = {
    l"""{ requires bridgeIds < maxBridges }"""
    bridges = bridges :+ bridge
    bridge.dispatchProtocol match {
      case DispatchPropertyProtocol.Periodic(period) =>
        AADLNative.logInfo(logTitle, s"Registered bridge: ${bridge.name} (periodic: $period)")
      case DispatchPropertyProtocol.Sporadic(min) =>
        AADLNative.logInfo(logTitle, s"Registered bridge: ${bridge.name} (sporadic: $min)")
    }
    for (port <- bridge.ports.all) {
      port.mode match {
        case PortMode.DataIn => AADLNative.logInfo(logTitle, s"- Registered port: ${port.name} (data in)")
        case PortMode.DataOut => AADLNative.logInfo(logTitle, s"- Registered port: ${port.name} (data out)")
        case PortMode.EventIn => AADLNative.logInfo(logTitle, s"- Registered port: ${port.name} (event in)")
        case PortMode.EventOut => AADLNative.logInfo(logTitle, s"- Registered port: ${port.name} (event out)")
      }
    }
  }

  // can't find definition in the standard ??
  def dispatchStatus(bridgeId: AADL.BridgeId): Option[PortId] = { // DISPATCH_STATUS
    val r = AADLNative.dispatchStatus(bridgeId)
    return r
  }

  def receiveInput(eventPortIdOpt: Option[PortId], dataPortIds: ISZ[PortId]): Unit = { // RECEIVE_INPUT
    AADLNative.receiveInput(eventPortIdOpt, dataPortIds)
  }

  def putValue[T](portId: PortId, data: T): Unit = { // PUT_VALUE
    // record all port ids for sendOutput
    AADLNative.putValue[T](portId, data)
  }

  def getValue[T](portId: PortId): T = { // GET_VALUE
    val r = AADLNative.getValue[T](portId)
    return r
  }

  def sendOutput(eventPortIds: ISZ[AADL.PortId], dataPortIds: ISZ[AADL.PortId]): Unit = { // SEND_OUTPUT
    AADLNative.sendOutput(eventPortIds, dataPortIds)
  }

  def connect(from: Port, to: Port): Unit = {
    connections(from.id) = to.id
    AADLNative.logInfo(logTitle, s"Connected ports: ${from.name} -> ${to.name}")
  }
}

@ext object AADLNative {
  def dispatchStatus(bridgeId: AADL.BridgeId): Option[AADL.PortId] = $

  def receiveInput(eventPortIdOpt: Option[AADL.PortId], dataPortIds: ISZ[AADL.PortId]): Unit = $

  def putValue[T](portId: AADL.PortId, data: T): Unit = $

  def getValue[T](portId: AADL.PortId): T = $

  def sendOutput(eventPortIds: ISZ[AADL.PortId], dataPortIds: ISZ[AADL.PortId]): Unit = $

  def logInfo(title: String, msg: String): Unit = $

  def logError(title: String, msg: String): Unit = $

  def logDebug(title: String, msg: String): Unit = $

  def logInfo(bridgeId: AADL.BridgeId, msg: String): Unit = $

  def logError(bridgeId: AADL.BridgeId, msg: String): Unit = $

  def logDebug(bridgeId: AADL.BridgeId, msg: String): Unit = $
}
