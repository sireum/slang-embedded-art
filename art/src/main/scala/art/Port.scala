// #Sireum

package art

import org.sireum._

@enum object PortMode {
  'DataIn
  'DataOut
  'EventIn
  'EventOut
}

@datatype class Port(id: Art.PortId,
                     name: String,
                     mode: PortMode.Type)
