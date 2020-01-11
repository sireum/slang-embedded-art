// #Sireum

package art

import org.sireum._

@ext object ArtDebug {

  def injectPort(bridgeId: Art.BridgeId, port: Art.PortId, data: DataContent): Unit = $

  def registerListener(listener: ArtListener): Unit = $

  def setDebugObject[T](key: String, o: T): Unit = $

  def getDebugObject[T](key: String): Option[T] = $
}

@msig trait ArtListener {

  // lifecycle information
  def start(time: Art.Time): Unit
  def stop(time: Art.Time): Unit

  // communication information
  def output(src: Art.PortId, dst: Art.PortId, data: DataContent, time: Art.Time): Unit

  // debug information (users can use this to send special messages which can be seen on inspector only)
//  def debug(message: String, time: Z)
}