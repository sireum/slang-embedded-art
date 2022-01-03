package art

import org.sireum._
import art.scheduling.Scheduler
import org.sireum.$internal.###

object ArtNative_Ext {

  var artNativeImpl: ArtNativeInterface = _

  ###(!("true" == System.getenv("PROYEK_JS") || scala.util.Try(Class.forName("scala.scalajs.js.Any", false, getClass.getClassLoader)).isSuccess)) {
    // targeting JVM
    artNativeImpl = new ArtNativeJVM()
  }

  ###("true" == System.getenv("PROYEK_JS") || scala.util.Try(Class.forName("scala.scalajs.js.Any", false, getClass.getClassLoader)).isSuccess) {
    // targeting ScalaJS
    artNativeImpl = ArtNativeSlang()
  }

  def shouldDispatch(bridgeId: Art.BridgeId): B = {
    return artNativeImpl.shouldDispatch(bridgeId)
  }

  def dispatchStatus(bridgeId: Art.BridgeId): DispatchStatus = {
    return artNativeImpl.dispatchStatus(bridgeId)
  }


  def receiveInput(eventPortIds: ISZ[Art.PortId], dataPortIds: ISZ[Art.PortId]): Unit = {
    artNativeImpl.receiveInput(eventPortIds, dataPortIds)
  }

  def putValue(portId: Art.PortId, data: DataContent): Unit = {
    artNativeImpl.putValue(portId, data)
  }

  def getValue(portId: Art.PortId): Option[DataContent] = {
    return artNativeImpl.getValue(portId)
  }

  def sendOutput(eventPortIds: ISZ[Art.PortId], dataPortIds: ISZ[Art.PortId]): Unit = {
    artNativeImpl.sendOutput(eventPortIds, dataPortIds)
  }


  def logInfo(title: String, msg: String): Unit = {
    artNativeImpl.logInfo(title, msg)
  }

  def logError(title: String, msg: String): Unit = {
    artNativeImpl.logError(title, msg)
  }

  def logDebug(title: String, msg: String): Unit = {
    artNativeImpl.logDebug(title, msg)
  }


  def tearDownSystemState(): Unit = {
    artNativeImpl.tearDownSystemState()
  }

  def setUpSystemState(): Unit = {
    artNativeImpl.setUpSystemState()
  }

  // JH: Refactor
  def initializePhase(): Unit = {
    artNativeImpl.initializePhase()
  }

  // JH: Refactor
  def computePhase(): Unit = {
    artNativeImpl.computePhase()
  }

  // JH: Refactor
  def finalizePhase(): Unit = {
    artNativeImpl.finalizePhase()
  }

  def time(): Art.Time = {
    artNativeImpl.time()
  }

  /////////////
  // TESTING //
  /////////////

  /**
   * Calls the initialize entry points on all registered bridges.
   * Testers should NOT call this method because BridgeTestSuite will automatically call this method before each test.
   *
   * (note: BridgeTestSuite exists only in the test scope)
   */
  def initTest(bridge: Bridge): Unit = {
    artNativeImpl.initTest(bridge)
  }

  /**
   * Precondition: executeInit() has been called prior.
   *
   * Executes the testCompute() method one time for each registered bridge.
   *
   * Unlike [[Art.run()]], this method does NOT wrap compute calls in a try-catch block.
   * This is to ensure no exceptions are overlooked during testing.
   */
  def executeTest(bridge: Bridge): Unit = {
    artNativeImpl.executeTest(bridge)
  }

  /**
   * Calls the finalize entry points on all registered bridges.
   * Testers should NOT call this method because BridgeTestSuite will automatically call this method after each test.
   *
   * (note: BridgeTestSuite exists only in the test scope)
   */
  def finalizeTest(bridge: Bridge): Unit = {
    artNativeImpl.finalizeTest(bridge)
  }

  // JH: Refactored
  //   add system test capability
  def initSystemTest(scheduler: Scheduler): Unit = {
    artNativeImpl.initSystemTest(scheduler)
  }

  //  def executeSystemTest(): Unit =

  // JH: Refactored
  //   add system test capability
  def finalizeSystemTest(): Unit = {
    artNativeImpl.finalizeSystemTest()
  }

  /**
   * A method that replaces bridge.compute()'s calls to [[Art.sendOutput()]] in its equivalent testCompute() method.
   *
   * This method is currently a NO-OP, but may gain functionality later.
   *
   * @param eventPortIds the event ports to be "copied and cleared" (but currently nothing happens)
   * @param dataPortIds the data ports to be "copied and cleared" (but currently nothing happens)
   */
  def releaseOutput(eventPortIds: ISZ[Art.PortId], dataPortIds: ISZ[Art.PortId]): Unit = {
    artNativeImpl.releaseOutput(eventPortIds, dataPortIds)
  }

  /**
   * Because a bridge's testCompute() doesn't clear outputs, this method can be used by users to manually
   * clear the output if desired. This is useful for tests involving multiple dispatches.
   */
  def manuallyClearOutput(): Unit = {
    artNativeImpl.manuallyClearOutput()
  }

  /**
   * Inserts a value into an "infrastructure in" port. For testing only, normally this is handled by Art.
   *
   * @param dstPortId the portId to place the passed [[DataContent]] into
   * @param data the [[DataContent]] which will be placed in the dstPort
   */
  def insertInPortValue(dstPortId: Art.PortId, data: DataContent): Unit = {
    artNativeImpl.insertInPortValue(dstPortId, data)
  }

  /**
   * Returns the value of an out port.
   *
   * @param portId the id of the OUTPUT port to return a value from
   * @return If the port is non-empty, a [[Some]] of [[DataContent]]. Otherwise [[None]].
   */
  def observeOutPortValue(portId: Art.PortId): Option[DataContent] = {
    return artNativeImpl.observeOutPortValue(portId)
  }

  // ** Manually added method by JH to support debugging interface

  /**
   * Returns the value of an in infrastructure port.
   *
   * @param portId the id of the INPUT infrastructure port to return a value from
   * @return If the port is non-empty, a [[Some]] of [[DataContent]]. Otherwise [[None]].
   */
  def observeInPortValue(portId: Art.PortId): Option[DataContent] = {
    return artNativeImpl.observeInPortValue(portId)
  }

  def observeOutPortVariable(portId: Art.PortId): Option[DataContent] = {
    return artNativeImpl.observeOutPortVariable(portId)
  }
}