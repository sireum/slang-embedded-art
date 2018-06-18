// #Sireum

package art

import org.sireum._

@sig trait DataContent

@datatype class Empty extends art.DataContent

@datatype class ArtPayload(data: DataContent,
                           received: Z) extends DataContent