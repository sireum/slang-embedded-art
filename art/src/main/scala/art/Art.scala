// #Sireum

package art

import org.sireum._

object Art {
  type PortId = N32
  type BridgeId = N32
  type Time = Z64
  type TimedDataContent = (Time, DataContent)

  val logTitle: String = "Art"
  val initIdVal: BridgeId = n32"0"
  val noTime: Time = z64"0"
  var bridgeIds: BridgeId = initIdVal
  var portIds: PortId = initIdVal
  var maxPorts: PortId = initIdVal
  var maxComponents: BridgeId = initIdVal
  var bridges: MS[BridgeId, Option[Bridge]] = MS[BridgeId, Option[Bridge]]()
  var connections: MS[PortId, PortId] = MS[PortId, PortId]()
  var lastSporadic: MS[BridgeId, Time] = MS[BridgeId, Time]()
  var eventPortVariables:  MS[PortId, Option[TimedDataContent]] = MS[PortId, Option[TimedDataContent]]()
  var dataPortVariables: MS[PortId, Option[TimedDataContent]] = MS[PortId, Option[TimedDataContent]]()
  var receivedPortValues: MS[PortId, Option[DataContent]] = MS[PortId, Option[DataContent]]()
  var sentPortValues: MS[PortId, Option[DataContent]] = MS[PortId, Option[DataContent]]()

  def initialise(numOfComponents: Z, numOfPorts: Z): Unit = {
    portIds = initIdVal
    bridgeIds = initIdVal
    maxComponents = Z.toN32(numOfComponents)
    maxPorts = Z.toN32(numOfPorts)
    bridges = MS.create[BridgeId, Option[Bridge]](maxComponents, None[Bridge]())
    lastSporadic = MS.create[BridgeId, Time](maxComponents, noTime)
    connections = MS.create[PortId, PortId](maxPorts, N32.Max)
    eventPortVariables = MS.create[PortId, Option[TimedDataContent]](maxPorts, None[TimedDataContent]())
    dataPortVariables = MS.create[PortId, Option[TimedDataContent]](maxPorts, None[TimedDataContent]())
    receivedPortValues = MS.create[PortId, Option[DataContent]](maxPorts, None[DataContent]())
    sentPortValues = MS.create[PortId, Option[DataContent]](maxPorts, None[DataContent]())
  }

  def bridge(bridgeId: BridgeId): Bridge = {
    val Some(r) = bridges(bridgeId)
    return r
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
        case Some((time, data)) =>
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
          eventPortVariables(portId) = None[TimedDataContent]()
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
