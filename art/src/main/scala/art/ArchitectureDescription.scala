// #Sireum

package art

import org.sireum._


@record class ArchitectureDescription(components: MSZ[Bridge],
                                      connections: ISZ[Connection])


@datatype class Connection(from: Port, to: Port)


@enum object PortMode {
  'DataIn
  'DataOut
  'EventIn
  'EventOut
}

@datatype class Port(id: Art.PortId,
                     name: String,
                     mode: PortMode.Type)


@sig trait Bridge {
  def id: Art.BridgeId

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

  @datatype class Ports(all: ISZ[Port],
                        dataIns: ISZ[Port],
                        dataOuts: ISZ[Port],
                        eventIns: ISZ[Port],
                        eventOuts: ISZ[Port])
}


@datatype trait DispatchPropertyProtocol

object DispatchPropertyProtocol {

  @datatype class Periodic(period: N32 /* hertz */) extends DispatchPropertyProtocol

  // @datatype class Aperiodic() extends DispatchPropertyProtocol

  @datatype class Sporadic(min: N32) extends DispatchPropertyProtocol

  // @datatype class Timed() extends DispatchPropertyProtocol

  // @datatype class Hybrid() extends DispatchPropertyProtocol
}
