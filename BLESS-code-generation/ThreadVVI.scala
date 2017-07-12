// #Sireum

package bless

import org.sireum._

/*
ThreadVVI is hand-translation of the VVI BLESS state machine into Slang
thread VVI
  features
    s: in event port;	--a ventricular contraction has been sensed
    p: out event port	--pace ventricle
      {BLESS::Assertion=>"<<VP()>>";};
    n: out event port	--non-refractory ventricular sense
      {BLESS::Assertion=>"<<(now=0) or VS()>>";};
    lrl: in data port BLESS_Types::Time;  --lower rate limit interval
    vrp: in data port BLESS_Types::Time;  --ventricular refractory period
  properties
    Dispatch_Protocol => Aperiodic;
annex BLESS
{**
assert
  <<notVRP: : --Ventricular Refractory Period
    (n or p)@last_beat --last beat before now,
    and ((now-last_beat)>=vrp)>>  --older than VRP
  <<VS: : --ventricular sense detected, not in VRP
    s@now and notVRP() >>
  <<VP: : --cause ventricular pace
    (n or p)@(now-lrl)  --last beat occurred LRL interval ago,
    and --not since then
      not (exists t:time  --there is no time
        in now-lrl,,now  --since then, ",," means open interval
        that (n or p)@t) >>  --with a beat
  <<PACE:x~time: --pace occurred in the previous LRL interval
    p@last_beat and  --previous beat was a pace
    (exists t:time  --there is a time
      in x-lrl..x  --in the previous LRL interval
      that p@t) >>  --with a ventricular pace
  <<SENSE:x~time:  --sense occurred in the previous LRL interval
    n@last_beat and  --previous beat was a sense
    (exists t:time  --there is a time
      in x-lrl..x  --in the previous LRL interval
      that n@t) >>  --with a non-refractory sense

invariant
  <<LRL(now)>>  --LRL is true, whenever "now" is

variables
  last_beat : time
    --the last pace or non-refractory sense occurred at last_beat
  <<LAST: :(n or p)@last_beat>>;

states
  power_on : initial state  --powered-up,
    <<now=0>>;	--start with "sense"
  pace : complete state
      --a ventricular pace has occured in the
      --previous LRL-interval milliseconds
    <<PACE(now)>>;
  sense : complete state
      --a ventricular sense has occured in the
      --previous LRL-interval milliseconds
    <<SENSE(now)>>;
  check_pace_vrp : state
      --execute state to check if s is in vrp after pace
    <<s@now and PACE(now)>>;
  check_sense_vrp : state
      --execute state to check if s is in vrp after sense
    <<s@now and SENSE(now)>>;
  off : final state;  --upon "stop"

transitions
  T1_POWER_ON:	--initialization
  power_on -[ ]-> sense
    {<<now=0>>
    n! <<n@now>>   --first "sense" at initialization
    & last_beat:=now <<last_beat=now>>};

  T3_PACE_LRL_AFTER_VP:	--pace when LRL times out
  pace -[on dispatch timeout (n or p) lrl ms]-> pace
    { <<VP()>>
    p! <<p@now>>   --cause pace when LRL times out
    & last_beat:=now <<last_beat=now>>};

  T4_VS_AFTER_VP:	--sense after pace=>check if in VRP
  pace -[on dispatch s]-> check_pace_vrp{};

  T5_VS_AFTER_VP_IN_VRP:  -- s in VRP,  go back to "pace" state
  check_pace_vrp -[(now-last_beat)<vrp]-> pace{};

  T6_VS_AFTER_VP_IS_NR:	--s after VRP,
    --go to "sense" state, send n!, reset timeouts
  check_pace_vrp -[(now-last_beat)>=vrp]-> sense
    {  <<VS()>>
    n! <<n@now>>  --send n! to reset timeouts
    & last_beat:=now <<last_beat=now>>};

  T7_PACE_LRL_AFTER_VS:	--pace when LRL times out after VS
  sense -[on dispatch timeout (p or n) lrl ms]-> pace
    {<<VP()>>
    p! <<p@now>>
    & last_beat:=now <<last_beat=now>>};

  T8_VS_AFTER_VS:	--check if s in VRP
  sense -[on dispatch s]-> check_sense_vrp{};

  T9_VS_AFTER_VS_IN_VRP:  -- s in VRP,  go back to "sense" state
  check_sense_vrp -[(now-last_beat)<vrp]-> sense{};

  T10_VS_AFTER_VS_IS_NR:  --s after VRP is non-refractory
  check_sense_vrp -[(now-last_beat)>=vrp]-> sense
    --reset timeouts with n! port send
    {  <<VS()>>
    n! <<n@now>>  --non-refractory ventricular sense
    & last_beat:=now <<last_beat=now>>};
**};	--end of annex subclause
end VVI;  --end thread
 */
@record class ThreadVVI() {

  // completeState holds the state entered when the thread last suspended
  var completeState: CompleteStates

  // persistent variable last_beat
  var last_beat: R;  //want to use type "time" here

  // only one transtition leaves initial state power_on, with default, true transition condition
  def initialize(ctx: VVI_Context): Unit = {
    ctx.Receive_Input()  //invoke AADL runtime service Receive_Input to freeze all incoming ports
    doT1_POWER_ON(ctx)
  }  //end of initialize

  // compute is the Compute_Entrypoint property for the thread
  // compute is invoked when the thread is dispatched
  def compute(ctx: VVI_Context): Unit = {
    ctx.Receive_Input()  //invoke AADL runtime service Receive_Input to freeze all incoming ports
    completeState match {
      case CompleteState_sense =>
        if (ctx.Updated(s)) //dispatch condition for T4_VS_AFTER_VP, event received on port s?
          doT4_VS_AFTER_VP(ctx)
        else if (ctx.Updated(timeout_p_or_n_lrl))  //dispatch condition for T3_PACE_LRL_AFTER_VP, timeout?
          doT3_PACE_LRL_AFTER_VP(ctx)
      case CompleteState_pace =>
        if (ctx.Updated(s)) //dispatch condition for T8_VS_AFTER_VS, event received on port s?
          doT8_VS_AFTER_VS(ctx)
        else if (ctx.timeout_p_or_n_lrl.Updated())  //dispatch condition for T7_PACE_LRL_AFTER_VS, timeout?
          doT7_PACE_LRL_AFTER_VS(ctx)
    }  //end of match
  }  //end of compute


  // transition leaving initial state power_on
  //   T1_POWER_ON:	power_on -[ ]-> sense
  def  doT1_POWER_ON(ctx: VVI_Context): Unit = {
    ctx.Put_Value(n)  // n!  send an event out port n
    last_beat = ctx.Now()  //put value of current time stamp into last_beat
    ctx.Send_Output()  //first transmit event
    //enter complete state sense
    completeState = sense  //set complete state to sense and suspend
  }  //end of doT1_POWER_ON

  // pace when LRL times out
  // T3_PACE_LRL_AFTER_VP: pace -[on dispatch timeout (n or p) lrl ms]-> pace
  def doT3_PACE_LRL_AFTER_VP(ctx: VVI_Context): Unit = {
    ctx.Put_Value(p)  // p!  send an event out port p
    last_beat = ctx.Now()  //put value of current time stamp into last_beat
    //enter complete state pace
    ctx.Send_Output()  //first transmit event
    completeState = pace  //set complete state to pace and suspend
  }  //end of doT3_PACE_LRL_AFTER_VP

  // T4_VS_AFTER_VP: pace -[on dispatch s]-> check_pace_vrp{};
  def doT4_VS_AFTER_VP(ctx: VVI_Context): Unit = {
    // sense after pace=>check if in VRP
    //execution state check_pace_vrp
    executionState_check_pace_vrp(ctx)
  }  //end of doT4_VS_AFTER_VP

  // T5_VS_AFTER_VP_IN_VRP: check_pace_vrp -[(now-last_beat)<vrp]-> pace
  def doT5_VS_AFTER_VP_IN_VRP(ctx: VVI_Context): Unit = {
    // s in VRP,  go back to "pace" state
    completeState = pace  //set complete state to pace and suspend
  }  //end of doT5_VS_AFTER_VP_IN_VRP

  // T6_VS_AFTER_VP_IS_NR: check_pace_vrp -[(now-last_beat)>=vrp]-> sense
  def doT6_VS_AFTER_VP_IS_NR(ctx: VVI_Context): Unit = {
    // s after VRP, go to "sense" state, send n!, reset timeouts
    ctx.Put_Value(n)  // n!  send an event out port n
    last_beat = ctx.Now()  //put value of current time stamp into last_beat
    ctx.Send_Output()  //first transmit event
    //enter complete state sense
    completeState = sense  //set complete state to sense and suspend
  }  //end of doT6_VS_AFTER_VP_IS_NR

  // T7_PACE_LRL_AFTER_VS: sense -[on dispatch timeout (p or n) lrl ms]-> pace
  def doT7_PACE_LRL_AFTER_VS(ctx: VVI_Context): Unit = {
    // pace when LRL times out after VS
    ctx.Put_Value(p)  // p!  send an event out port p
    last_beat = ctx.Now()  //put value of current time stamp into last_beat
    //enter complete state pace
    ctx.Send_Output()  //first transmit event
    completeState = pace  //set complete state to pace and suspend
  }  //end of doT7_PACE_LRL_AFTER_VS

 // T8_VS_AFTER_VS: sense -[on dispatch s]-> check_sense_vrp{};
  def doT8_VS_AFTER_VS(ctx: VVI_Context): Unit = {
    // sense after sense=>check if in VRP
    //execution state check_sense_vrp
    executionState_check_sence_vrp(ctx)
  }  //end of doT8_VS_AFTER_VS

 // T9_VS_AFTER_VS_IN_VRP: check_sense_vrp -[(now-last_beat)<vrp]-> sense{};
  def doT9_VS_AFTER_VS_IN_VRP(ctx: VVI_Context): Unit = {
    // s in VRP,  go back to "sense" state
    completeState = sense  //set complete state to sense and suspend
  }  //end of doT9_VS_AFTER_VS_IN_VRP

  // T10_VS_AFTER_VS_IS_NR: check_sense_vrp -[(now-last_beat)>=vrp]-> sense
  def doT10_VS_AFTER_VS_IS_NR(ctx: VVI_Context): Unit = {
    // s after VRP, go to "sense" state, send n!, reset timeouts
    ctx.Put_Value(n)  // n!  send an event out port n
    last_beat = ctx.Now()  //put value of current time stamp into last_beat
    ctx.Send_Output()  //first transmit event
    //enter complete state sense
    completeState = sense  //set complete state to sense and suspend
  }  //end of doT10_VS_AFTER_VS_IS_NR



  // execution state check_pace_vrp
  // T5_VS_AFTER_VP_IN_VRP: check_pace_vrp -[(now-last_beat)<vrp]-> pace
  // T6_VS_AFTER_VP_IS_NR: check_pace_vrp -[(now-last_beat)>=vrp]-> sense
  def executionState_check_pace_vrp(ctx: VVI_Context): Unit = {
    if ((ctx.Now()-last_beat) < ctx.vrp.Get_Value())
      doT5_VS_AFTER_VP_IN_VRP(ctx)
    else if ((ctx.Now()-last_beat) >= ctx.vrp.Get_Value())
      doT6_VS_AFTER_VP_IS_NR(ctx)
    else
    /* error */
  }  //end of executionState_check_pace_vrp

  // execution state check_sense_vrp
  // T9_VS_AFTER_VS_IN_VRP: check_sense_vrp -[(now-last_beat)<vrp]-> sense{};
  // T10_VS_AFTER_VS_IS_NR: check_sense_vrp -[(now-last_beat)>=vrp]-> sense
  def executionState_check_sense_vrp(ctx: VVI_Context): Unit = {
    if ((ctx.Now()-last_beat) < ctx.vrp.Get_Value())
      doT9_VS_AFTER_VS_IN_VRP(ctx)
    else if ((ctx.Now()-last_beat) >= ctx.vrp.Get_Value())
      doT10_VS_AFTER_VS_IS_NR(ctx)
    else
    /* error */
  }  //end of executionState_check_sense_vrp

}  //end of ThreadVVI



//CompleteStates lists the complete states in the VVI state machine
@enum object CompleteStates {
  'CompleteState_pace
  'CompleteState_sense
}  //end of CompleteStates

//Context object for VVI
@record class //trait?
  VVI_Context extends AADL_Context {

  val s = Uport[s,"s",EventIn]
  val n = Uport[n,"n",EventOut]
  val p = Uport[p,"p",EventOut]
  val timeout_p_or_n_lrl = Uport[timeout_p_or_n_lrl,"timeout_p_or_n_lrl",EventIn]
  val lrl = Uport[lrl,"lrl",DataIn]
  val vrp = Uport[vrp,"vrp",DataIn]

  def ports: Bridge.Ports = {all: s n p lrl vrp, eventIns: s timeout_p_or_n_lrl, eventOuts: n p, dataIns: lrl vrp}

}  //end of VVI_Context


