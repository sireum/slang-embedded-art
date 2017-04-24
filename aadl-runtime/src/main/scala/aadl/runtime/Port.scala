// #Sireum

package aadl.runtime

import org.sireum._

@enum object PortMode {
  'DataIn
  'DataOut
  'EventIn
  'EventOut
}

@datatype class Port(id: AADL.PortId,
                     name: String,
                     mode: PortMode.Type)
