// #Sireum

package art

import org.sireum._

object Art {
  type PortId = N32
  type BridgeId = N32
  type Time = Z64
  type TimedContent = (Time, DataContent)

  val maxComponents: PortId = n32"3" // constant set during instantiation, must be < N32.Max
  val maxPorts: PortId = n32"7" // constant set during instantiation, must be < N32.Max

  val noTime: Time = z64"0"

  val logTitle: String = "Art"
  val bridges: MS[BridgeId, Option[Bridge]] = MS.create[BridgeId, Option[Bridge]](maxComponents, None[Bridge]())
  val connections: MS[PortId, PortId] = MS.create[PortId, PortId](maxPorts, N32.Max)
  val lastSporadic: MS[BridgeId, Time] = MS.create[BridgeId, Time](maxComponents, noTime)
  val eventPortVariables: MS[PortId, Option[TimedContent]] =
    MS.create[PortId, Option[TimedContent]](maxPorts, None[TimedContent]())
  val dataPortVariables: MS[PortId, Option[TimedContent]] =
    MS.create[PortId, Option[TimedContent]](maxPorts, None[TimedContent]())
  val receivedPortValues: MS[PortId, Option[DataContent]] =
    MS.create[PortId, Option[DataContent]](maxPorts, None[DataContent]())
  val sentPortValues: MS[PortId, Option[DataContent]] =
    MS.create[PortId, Option[DataContent]](maxPorts, None[DataContent]())

  def bridge(bridgeId: BridgeId): Bridge = {
    val Some(r) = bridges(bridgeId)
    return r
  }

  def register(bridge: Bridge): Unit = {
    l"""{ requires bridgeIds < maxBridges }"""
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
  def dispatchStatus(bridgeId: Art.BridgeId): Option[PortId] = { // DISPATCH_STATUS
    var minPortId = n32"0"
    var minTime = Z64.Max
    for (port <- bridge(bridgeId).ports.eventIns) {
      val portId = port.id
      eventPortVariables(portId) match {
        case Some((time, _)) =>
          if (time < minTime) {
            minTime = time
            minPortId = portId
          }
        case _ =>
      }
    }
    return if (minTime == Z64.Max) None[PortId]() else Some(minPortId)
  }

  def receiveInput(eventPortIdOpt: Option[PortId], dataPortIds: ISZ[PortId]): Unit = { // RECEIVE_INPUT
    for (portId <- eventPortIdOpt) {
      eventPortVariables(portId) match {
        case Some((_, data)) =>
          eventPortVariables(portId) = None[TimedContent]()
          receivedPortValues(portId) = Some(data)
        case _ =>
      }
    }
    for (portId <- dataPortIds) {
      dataPortVariables(portId) match {
        case Some((_, data)) =>
          receivedPortValues(portId) = Some(data)
        case _ =>
      }
    }
  }

  def putValue(portId: PortId, data: DataContent): Unit = { // PUT_VALUE
    sentPortValues(portId) = Some(data)
  }

  def getValue(portId: PortId): DataContent = { // GET_VALUE
    val Some(data) = receivedPortValues(portId)
    return data
  }

  def sendOutput(eventPortIds: ISZ[Art.PortId], dataPortIds: ISZ[Art.PortId]): Unit = { // SEND_OUTPUT
    val time = ArtNative.time()
    for (portId <- eventPortIds) {
      sentPortValues(portId) match {
        case Some(data) =>
          eventPortVariables(Art.connections(portId)) = Some((time, data))
          sentPortValues(portId) = None[DataContent]()
        case _ =>
      }
    }
    for (portId <- eventPortIds) {
      sentPortValues(portId) match {
        case Some(data) =>
          dataPortVariables(Art.connections(portId)) = Some((time, data))
          sentPortValues(portId) = None[DataContent]()
        case _ =>
      }
    }
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

  def connect(from: Port, to: Port): Unit = {
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
