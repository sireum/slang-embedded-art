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

  def init(numOfPorts: Z, numOfBridges: Z): Unit = {
    portIds = initIdVal
    bridgeIds = initIdVal
    maxPorts = Z.toN32(numOfPorts)
    maxBridges = Z.toN32(numOfBridges)
    bridges = MS[BridgeId, Bridge]()
    lastSporadic = MS.create[BridgeId, Z64](maxBridges, z64"0")
    connections = MS.create[PortId, PortId](maxPorts, initIdVal)
  }

  def registerPort(name: String, port: Port): PortId = {
    l"""{ requires portIds < maxPorts }"""
    val r = portIds
    portIds = r + n32"1"
    AADLExt.logInfo(logTitle, s"Registered port: $name (#$r)")
    return r
  }

  def registerBridge(bridge: Bridge): BridgeId = {
    l"""{ requires bridgeIds < maxBridges }"""
    val r = bridgeIds
    bridge.setId(r)
    bridges = bridges :+ bridge
    bridgeIds = r + n32"1"
    bridge.dispatchProtocol match {
      case Bridge.DispatchPropertyProtocol.Periodic(period) =>
        AADLExt.logInfo(logTitle, s"Registered bridge: ${bridge.name} (#$r, periodic: $period)")
      case Bridge.DispatchPropertyProtocol.Sporadic(min) =>
        AADLExt.logInfo(logTitle, s"Registered bridge: ${bridge.name} (#$r, sporadic: $min)")
    }
    return r
  }

  // can't find definition in the standard ??
  def dispatchStatus(bridgeId: AADL.BridgeId): Option[PortId] = { // DISPATCH_STATUS
    val r = AADLExt.dispatchStatus(bridgeId)
    return r
  }

  def receiveInput(eventPortIdOpt: Option[PortId], dataPortIds: ISZ[PortId]): Unit = { // RECEIVE_INPUT
    AADLExt.receiveInput(eventPortIdOpt, dataPortIds)
  }

  def putValue[T](portId: PortId, data: T): Unit = { // PUT_VALUE
    // record all port ids for sendOutput
    AADLExt.putValue[T](portId, data)
  }

  def getValue[T](portId: PortId): T = { // GET_VALUE
    val r = AADLExt.getValue[T](portId)
    return r
  }

  def sendOutput(eventPortIds: ISZ[AADL.PortId], dataPortIds: ISZ[AADL.PortId]): Unit = { // SEND_OUTPUT
    AADLExt.sendOutput(eventPortIds, dataPortIds)
  }

  def connect(from: PortId, to: PortId): Unit = {
    connections(from) = to
    AADLExt.logInfo(logTitle, s"Connected ports: $from -> $to")
  }

  @sig trait Port

  @sig trait Bridge {
    def name: String
    def id: BridgeId
    def setId(id: BridgeId): Unit
    def entryPoints: Bridge.EntryPoints
    def dispatchProtocol: Bridge.DispatchPropertyProtocol
    def portIds: ISZ[PortId]
    def inPortIds: ISZ[PortId]
    def outPortIds: ISZ[PortId]
    def dataPortIds: ISZ[PortId]
    def eventPortIds: ISZ[PortId]
  }

  object Bridge {

    // initialise()  ( compute() | activate() deactivate() | recover() )* finalise()
    @sig trait EntryPoints {
      def initialise(): Unit

      def activate(): Unit

      def deactivate(): Unit

      def compute(): Unit

      def recover(): Unit

      def finalise(): Unit
    }

    @datatype trait DispatchPropertyProtocol

    object DispatchPropertyProtocol {

      @datatype class Periodic(period: N32 /* hertz */) extends DispatchPropertyProtocol

      // @datatype class Aperiodic() extends DispatchPropertyProtocol

      @datatype class Sporadic(min: N32) extends DispatchPropertyProtocol

      // @datatype class Timed() extends DispatchPropertyProtocol

      // @datatype class Hybrid() extends DispatchPropertyProtocol
    }
  }
}

@ext object AADLExt {
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
