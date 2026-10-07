// #Sireum

package art

import org.sireum._
import org.sireum.S64._

/** Conversions to Art.Time, which is in nanoseconds, e.g. ArtTimer.schedule(id, T, ArtTime.millis(500), cb).
  * The multiplication is done in S64, not Z, as Z may be 32 bits wide in transpiled C. */
object ArtTime {

  @strictpure def millis(n: Z): Art.Time = conversions.Z.toS64(n) * s64"1000000"

  @strictpure def micros(n: Z): Art.Time = conversions.Z.toS64(n) * s64"1000"
}
