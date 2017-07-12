// #Sireum

package bless

import art._
import org.sireum._

/*
AADL_Context is the parent class for all ThreadName_Context objects, which
supplied AADL runtime services for each thread
 */
@record class AADL_Context {

  //ports uses Bridge.Ports in ArchitectureDescription to identify ports of a thread
  def ports: Bridge.Ports

  // Send_Output:explicitlycauseevents,eventdata,ordatatobetransmittedthroughoutgoingportstoreceiverports.
  def Send_Outputs(): Unit

  // Put_Value: allowsthesourcetextofathreadtosupplyadatavaluetoaportvariable.
  def Put_Value(portId: PortId, data: DataContent): Unit = { // PUT_VALUE
    Art.putValue(portId, data)
  }

  // Receive_Input: explicitly requests port input on s incoming ports to be frozen and made accessible through the port variables.
  def receiveInput(): Unit = { // RECEIVE_INPUT
    Art.receiveInput(ports.eventIns, ports.dataIns)
  }


  // Get_Value: allows access to the current value of a port variable.
  def Get_Value(portId: PortId): DataContent = { // GET_VALUE
    val r = Art.getValue(portId)
    return r
  }

  // Get_Count: determine whether a new data value is available on a port variable, and in case of queued event and event data ports, how many elements are available to the thread in the queue.
  def Get_Count(portId: PortId): N  //extract from Bridge.dispatchStatus  ???


  // Next_Value: provides access to the next queued element of a port variable as the current value. A NoValue exception is raised if no more values are available.
  def Next_Value(portId: PortId): Unit  //extract from Bridge.dispatchStatus  ???

  // Updated allows the source text of a thread to determine whether input has been transmitted to a port since the last Receive_Input service call.
  def Updated(portId: PortId): B  //extract from Bridge.dispatchStatus  ???

  //now pulls a current timestamp
  def Now(): R  //want to have type "time" native


}  //end of AADL_Context

