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
  val noTimeEmptyContent: (Time, DataContent) = (noTime, EmptyDataContent())
  var bridgeIds: BridgeId = initIdVal
  var portIds: PortId = initIdVal
  var maxPorts: PortId = initIdVal
  var maxComponents: BridgeId = initIdVal
  var bridges: MS[BridgeId, Bridge] = MS[BridgeId, Bridge]()
  var connections: MS[PortId, PortId] = MS[PortId, PortId]()
  var lastSporadic: MS[BridgeId, Time] = MS[BridgeId, Time]()
  var eventPortVariables: MS[PortId, TimedDataContent] = MS[PortId, TimedDataContent]()
  var dataPortVariables: MS[PortId, TimedDataContent] = MS[PortId, TimedDataContent]()
  var receivedPortValues: MS[PortId, DataContent] = MS[PortId, DataContent]()
  var sentPortValues: MS[PortId, DataContent] = MS[PortId, DataContent]()

  def initialise(numOfComponents: Z, numOfPorts: Z): Unit = {
    portIds = initIdVal
    bridgeIds = initIdVal
    maxComponents = Z.toN32(numOfComponents)
    maxPorts = Z.toN32(numOfPorts)
    bridges = MS[BridgeId, Bridge]()
    lastSporadic = MS.create[BridgeId, Time](maxComponents, noTime)
    connections = MS.create[PortId, PortId](maxPorts, initIdVal)
    eventPortVariables = MS.create[PortId, TimedDataContent](maxPorts, noTimeEmptyContent)
    dataPortVariables = MS.create[PortId, TimedDataContent](maxPorts, noTimeEmptyContent)
    receivedPortValues = MS.create[PortId, DataContent](maxPorts, EmptyDataContent())
    sentPortValues = MS.create[PortId, DataContent](maxPorts, EmptyDataContent())
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
    var minTime = noTime
    for (port <- Art.bridges(bridgeId).ports.eventIns) {
      val portId = port.id
      val (time, data) = eventPortVariables(portId)
      if (data.nonEmpty & (minTime == noTime | time < minTime)) {
        minTime = time
        minPortId = portId
      }
    }
    return if (minTime == noTime) None[PortId]() else Some(minPortId)
  }

  def receiveInput(eventPortIdOpt: Option[PortId], dataPortIds: ISZ[PortId]): Unit = { // RECEIVE_INPUT
    for (portId <- eventPortIdOpt) {
      val v = eventPortVariables(portId)._2
      eventPortVariables(portId) = noTimeEmptyContent
      receivedPortValues(portId) = v
    }
    for (portId <- dataPortIds) {
      receivedPortValues(portId) = dataPortVariables(portId)._2
    }
  }

  def putValue(portId: PortId, data: DataContent): Unit = { // PUT_VALUE
    sentPortValues(portId) = data
  }

  def getValue(portId: PortId): DataContent = { // GET_VALUE
    return receivedPortValues(portId)
  }

  def sendOutput(eventPortIds: ISZ[Art.PortId], dataPortIds: ISZ[Art.PortId]): Unit = { // SEND_OUTPUT
    val time = ArtNative.time()
    for (portId <- eventPortIds) {
      val data = sentPortValues(portId)
      if (data.nonEmpty) {
        eventPortVariables(Art.connections(portId)) = (time, data)
        sentPortValues(portId) = EmptyDataContent()
      }
    }
    for (portId <- eventPortIds) {
      val data = sentPortValues(portId)
      if (data.nonEmpty) {
        dataPortVariables(Art.connections(portId)) = (time, data)
        sentPortValues(portId) = EmptyDataContent()
      }
    }
  }

  def logInfo(bridgeId: Art.BridgeId, msg: String): Unit = {
    ArtNative.logInfo(bridges(bridgeId).name, msg)
  }

  def logError(bridgeId: Art.BridgeId, msg: String): Unit = {
    ArtNative.logError(bridges(bridgeId).name, msg)
  }

  def logDebug(bridgeId: Art.BridgeId, msg: String): Unit = {
    ArtNative.logDebug(bridges(bridgeId).name, msg)
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
