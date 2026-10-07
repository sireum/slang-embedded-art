// #Sireum

package art

import org.sireum._

@sig trait TimerCallback {
  def callback(): Unit
}

@ext object ArtTimer {

  // delayNs is in nanoseconds; use ArtTime.millis/micros to convert, e.g. ArtTime.millis(500)
  def schedule(id: String, replaceExisting: B, delayNs: Art.Time, callback: () => Unit): Unit = $

  // if transpiling then use this version as transpiler does not support function passing
  def scheduleTrait(id: String, replaceExisting: B, delayNs: Art.Time, callback: TimerCallback): Unit = $

  def cancel(id: String): Unit = $
}
