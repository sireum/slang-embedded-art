package aadl.runtime

import org.sireum._

object AADLExt_Ext {
  def dispatchStatus: Option[AADL.PortId] = {
    ???
  }

  def receiveInput(portIds: ISZ[AADL.PortId]): Unit = {
    ???
  }

  def putValue[T](portId: AADL.PortId, data: T): Unit = {
    ???
  }

  def getValue[T](portId: AADL.PortId): T = {
    ???
  }

  def sendOutput(): Unit = {
    ???
  }

  def registerBridge(bridge: AADL.Bridge): Unit = {
    ???
  }

  def connect(from: AADL.PortId, to: AADL.PortId): Unit = {
    ???
  }

  def logInfo(bridge: AADL.Bridge, msg: String): Unit = {
    ???
  }

  def logError(bridge: AADL.Bridge, msg: String): Unit = {
    ???
  }

  def logDebug(bridge: AADL.Bridge, msg: String): Unit = {
    ???
  }

  def run(): Unit = {

  }
}
