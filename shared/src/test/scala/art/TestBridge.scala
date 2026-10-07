// #Sireum

package art

import org.sireum._

// a bridge with no behaviour, for ART's own unit tests
@datatype class TestBridge(val id: Art.BridgeId,
                           val name: String,
                           val ports: Bridge.Ports,
                           val entryPoints: Bridge.EntryPoints,
                           val dispatchProtocol: DispatchPropertyProtocol) extends Bridge

@datatype class TestEntryPoints() extends Bridge.EntryPoints {
  def initialise(): Unit = {}

  def compute(): Unit = {}

  def finalise(): Unit = {}
}
