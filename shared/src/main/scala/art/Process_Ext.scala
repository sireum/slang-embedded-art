package art

import org.sireum._
import org.sireum.S64._

object Process_Ext {
  def time(): Art.Time = {
    return S64(System.currentTimeMillis())
  }
}
