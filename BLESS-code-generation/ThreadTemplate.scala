// #Sireum

package bless

import org.sireum._

/*
ThreadTemplate holds the structure of Slang code generated from BLESS thread behaviors
 */
@record class ThreadTemplate() {

  //completeState holds the state entered when the thread last suspended
  var completeState: CompleteStates

  //persistent variables are declared, and intialized here
  var localVariable : Z

  //initialize is the Initialize_Entrypoint property for the thread
  //there may be more than one execution condition leaving the initial state
  def initialize(ctx: AADL_Context): Unit = {
    ctx.Receive_Input()  //invoke AADL runtime service Receive_Input to freeze all incoming ports
    if (/*transition condition for T1 */)
      doT1(ctx)
    else if (/*transition condition for T2 */)
      doT2(ctx)
    else
      /* error; there must be an enabled outgoing transition from the initial state */
  }  //end of initialize

  //compute is the Compute_Entrypoint property for the thread
  //compute is invoked when the thread is dispatched
  def compute(ctx: AADL_Context): Unit = {
    //execution depends on completeState saved just before last suspension of the thread
    ctx.Receive_Input()  //invoke AADL runtime service Receive_Input to freeze all incoming ports
    completeState match {
      case CompleteStateA =>  //look for dispatch conditions of transitions leaving CompleteStateA
        if (/* transition condition for TA1 */)
          doTA1(ctx)  //take transition TA1
        else if (/* transition condition for TA2 */)
          doTA2(ctx)  //take transition TA2
        //otherwise, no dispatch condition leaving CompleteState_A is enabled, go back to sleep
      case CompleteState_B =>  //look for dispatch conditions of transitions leaving CompleteState_B

      case CompleteState_C =>  //look for dispatch conditions of transitions leaving CompleteState_C

      case _ =>  //otherwise error; there must be a case for each complete state
    }  //end of match
  }  //end of compute

  //transition T1 is taken, destination is a complete state
  //  T1: A -[ dipatch condition ]-> B {action};
  def doT1 (ctx: AADL_Context): Unit = {
    /* do action for T1, if any */
    ctx.Send_Output()  //invoke AADL runtime service Send_Output to explicitly cause events, event data, or data to be transmitted through outgoing ports to receiver ports
    //the desitnation state of T1 is CompleteState_B
    completeState = CompleteState_B
    //suspend execution until next dispatch
  }  //end of doT1

  //transition T2 is taken, destination is an execution state
  //  T2: A -[ dipatch condition ]-> X {action};
  def doT2 (ctx: AADL_Context): Unit = {
    /* do action for T2, if any */
    //the desitnation state of T2 is execution state X
    executionStateX(ctx)
  }  //end of doT2


  //execution state X has three outgoing transitions
  //  T3: X -[ execution condition ]-> A {action};
  //  T4: X -[ execution condition ]-> Y {action};
  //  T5: X -[ execution condition ]-> W {action};
  def executionState_X (ctx: AADL_Context): Unit = {
    if (/* execution condition for T3 */)
      doT3(ctx)
    else if (/* execution condition for T4 */)
      doT4(ctx)
    else if (/* execution condition for T5 */)
      doT5(ctx)
    else
      /* error; there must be an enabled outgoint transition from X */
  }  //end of executionStateX

  // transitions T3, T4, and T5 leave state X
  //  T3: X -[ execution condition ]-> A {action};
  def doT3 (ctx: AADL_Context): Unit = {
  }  //end of T3
  //  T4: X -[ execution condition ]-> Y {action};
  def doT4 (ctx: AADL_Context): Unit = {
  }  //end of T4
  //  T5: X -[ execution condition ]-> W {action};
  def doT5 (ctx: AADL_Context): Unit = {
  }  //end of T5



  }  //end of ThreadTemplate

//CompleteStates lists the complete states in the BLESS state machine
@enum object CompleteStates {
  'CompleteState_A
  'CompleteState_B
  'CompleteState_C
}  //end of CompleteStates

