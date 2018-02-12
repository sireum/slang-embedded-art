// #Sireum

package art

import org.sireum._

@ext object ArtNative {

  def dispatchStatus(bridgeId: Art.BridgeId): DispatchStatus = $

  def receiveInput(eventPortIds: ISZ[Art.PortId], dataPortIds: ISZ[Art.PortId]): Unit = $

  def putValue(portId: Art.PortId, data: DataContent): Unit = $

  def getValue(portId: Art.PortId): Option[DataContent] = $

  def sendOutput(eventPortIds: ISZ[Art.PortId], dataPortIds: ISZ[Art.PortId]): Unit = $

  def registerPortListener(portId: Art.PortId, callback: DataContent => Unit): Unit = $

  def injectPort(bridgeId: Art.BridgeId, port: Art.PortId, data: DataContent): Unit = $

  def setDebugObject[T](key: String, o: T): Unit = $

  def getDebugObject[T](key: String): Option[T] = $

  def logInfo(title: String, msg: String): Unit = $

  def logError(title: String, msg: String): Unit = $

  def logDebug(title: String, msg: String): Unit = $

  def run(): Unit = $

  def time(): Art.Time = $
}
