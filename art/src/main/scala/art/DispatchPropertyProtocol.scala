// #Sireum

package art

import org.sireum._

@datatype trait DispatchPropertyProtocol

object DispatchPropertyProtocol {

  @datatype class Periodic(period: N32 /* hertz */) extends DispatchPropertyProtocol

  // @datatype class Aperiodic() extends DispatchPropertyProtocol

  @datatype class Sporadic(min: N32) extends DispatchPropertyProtocol

  // @datatype class Timed() extends DispatchPropertyProtocol

  // @datatype class Hybrid() extends DispatchPropertyProtocol
}
