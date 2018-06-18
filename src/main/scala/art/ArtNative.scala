// #Sireum

package art

import org.sireum._

@ext object ArtNative {

  def dispatchStatus(bridgeId: Art.BridgeId): DispatchStatus = $

  def receiveInput(eventPortIds: ISZ[Art.PortId], dataPortIds: ISZ[Art.PortId]): Unit = $

  def putValue(portId: Art.PortId, data: DataContent): Unit = $

  def getValue(portId: Art.PortId): Option[DataContent] = $

  def fresh(portId: Art.PortId) : B = $

  def updated(portId: Art.PortId) : B = $

  def sendOutput(eventPortIds: ISZ[Art.PortId], dataPortIds: ISZ[Art.PortId]): Unit = $

  def logInfo(title: String, msg: String): Unit = $

  def logError(title: String, msg: String): Unit = $

  def logDebug(title: String, msg: String): Unit = $

  def run(): Unit = $

  def time(): Art.Time = $
}
