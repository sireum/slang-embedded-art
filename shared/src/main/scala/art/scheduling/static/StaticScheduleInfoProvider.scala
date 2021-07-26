// #Sireum

package art.scheduling.static

import org.sireum._

@sig trait StaticScheduleInfoProvider {

  def bridgeNickNameToId(name: String): art.Art.PortId
  def bridgeIdToNickName(bridgeId: art.Art.BridgeId): String

  def portNickNameToId(portId: art.Art.PortId): String
  def portIdToNickName(name: String): art.Art.PortId
}
