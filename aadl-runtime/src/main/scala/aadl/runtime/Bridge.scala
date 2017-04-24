// #Sireum

package aadl.runtime

import org.sireum._

@sig trait Bridge {
  def id: AADL.BridgeId

  def name: String

  def ports: Bridge.Ports

  def entryPoints: Bridge.EntryPoints

  def dispatchProtocol: DispatchPropertyProtocol
}

object Bridge {

  // initialise()  ( compute() | activate() deactivate() | recover() )* finalise()
  @sig trait EntryPoints {
    def initialise(): Unit

    def activate(): Unit

    def deactivate(): Unit

    def compute(): Unit

    def recover(): Unit

    def finalise(): Unit
  }

  @datatype class Ports(all: ISZ[Port]) {
    def mode(m: PortMode.Type): ISZ[Port] = {
      var r = ISZ[Port]()
      for (port <- all) {
        if (port.mode == m) {
          r = r :+ port
        }
      }
      return r
    }
  }

}
