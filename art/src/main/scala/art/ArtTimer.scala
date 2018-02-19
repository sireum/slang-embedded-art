// #Sireum

package art

import org.sireum._
import art.Art.BridgeId

@ext object ArtTimer {

  def setTimeout(bridgeId: BridgeId, eventId: String, wait: Art.Time, callback: () => Unit): Unit = $

  def clearTimeout(eventId: String): Unit = $
}
