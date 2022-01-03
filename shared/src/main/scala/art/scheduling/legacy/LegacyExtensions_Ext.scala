package art.scheduling.legacy

import art.Bridge
import org.sireum._
import org.sireum.$internal.###

trait LegacyInterface {
  def computePhase(bridges: ISZ[art.Bridge]): Unit
}

class LegacyScalaJSInterface() extends  LegacyInterface {
  def computePhase(bridges: _root_.org.sireum.ISZ[Bridge]): Unit = {
    halt("The legacy scheduler is not ScalaJS compliant")
  }
}

object LegacyExtensions_Ext {

  var legacyInterfaceImpl: LegacyInterface = _

  ###(!("true" == System.getenv("PROYEK_JS") || scala.util.Try(Class.forName("scala.scalajs.js.Any", false, getClass.getClassLoader)).isSuccess)) {
    // targeting JVM
    legacyInterfaceImpl = new LegacyJVMInterface()
  }

  ###("true" == System.getenv("PROYEK_JS") || scala.util.Try(Class.forName("scala.scalajs.js.Any", false, getClass.getClassLoader)).isSuccess) {
    // targeting ScalaJS
    legacyInterfaceImpl = new LegacyScalaJSInterface()
  }

  def computePhase(bridges: ISZ[art.Bridge]): Unit = {
    legacyInterfaceImpl.computePhase(bridges)
  }
}
