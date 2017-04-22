// #Sireum

package aadl.runtime

import org.sireum._

object AADL {
  type PortId = N32
  val logTitle = "AADL Runtime"
  val portIdInit = n32"0"
  var ports: N32 = portIdInit

  // can't find definition in the standard ??
  def dispatchStatus(bridge: AADL.Bridge): Option[PortId] = { // DISPATCH_STATUS
    val r = AADLExt.dispatchStatus(bridge)
    return r
  }

  def receiveInput(portIds: ISZ[PortId]): Unit = { // RECEIVE_INPUT
    AADLExt.receiveInput(portIds)
  }

  def putValue[T](portId: PortId, data: T): Unit = { // PUT_VALUE
    // record all port ids for sendOutput
    AADLExt.putValue[T](portId, data)
  }

  def getValue[T](portId: PortId): T = { // GET_VALUE
    val r = AADLExt.getValue[T](portId)
    return r
  }

  // standard calls for a list of ports
  // but putValue should build the list dynamically
  def sendOutput(): Unit = { // SEND_OUTPUT
    AADLExt.sendOutput()
  }

  def registerPort(name: String, port: Port): PortId = {
    assume(ports < N32.Max - n32"1")
    val r = ports + n32"1"
    ports = r
    AADLExt.logInfo(logTitle, s"Registered port: $name (#$r)")
    return r
  }

  def registerBridge(bridge: Bridge): Unit = {
    AADLExt.registerBridge(bridge)
  }

  def connect(from: PortId, to: PortId): Unit = {
    AADLExt.connect(from, to)
    AADLExt.logInfo(logTitle, s"Connected ports: $from -> $to")
  }

  @sig trait Port

  @sig trait Bridge {
    def name: String
    def entryPoints: Bridge.EntryPoints
    def dispatchProtocol: Bridge.DispatchPropertyProtocol
    def portIds: ISZ[PortId]
    def inPortIds: ISZ[PortId]
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
  def dispatchStatus(bridge: AADL.Bridge): Option[AADL.PortId] = $
  def receiveInput(portIds: ISZ[AADL.PortId]): Unit = $
  def putValue[T](portId: AADL.PortId, data: T): Unit = $
  def getValue[T](portId: AADL.PortId): T = $
  def sendOutput(): Unit = $
  def registerBridge(bridge: AADL.Bridge): Unit = $
  def connect(from: AADL.PortId, to: AADL.PortId): Unit = $
  def logInfo(title: String, msg: String): Unit = $
  def logError(title: String, msg: String): Unit = $
  def logDebug(title: String, msg: String): Unit = $
  def logInfo(bridge: AADL.Bridge, msg: String): Unit = $
  def logError(bridge: AADL.Bridge, msg: String): Unit = $
  def logDebug(bridge: AADL.Bridge, msg: String): Unit = $
}
