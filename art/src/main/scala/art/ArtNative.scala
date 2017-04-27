// #Sireum

package art

import org.sireum._

@ext object ArtNative {
  def logInfo(title: String, msg: String): Unit = $

  def logError(title: String, msg: String): Unit = $

  def logDebug(title: String, msg: String): Unit = $

  def time(): Art.Time = $

  def run(): Unit = $
}
