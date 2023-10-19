// #Sireum
package art.scheduling.static

import org.sireum._
import art.Art.BridgeId

object CommandInterpreter {

  def makeHelpMessage(): String = {
    return (
      st"""s <n?>     - step n slots (default 0)
          |h <n?>     - step n hyper-periods (default 0)
          |rs <n>     - run to slot n (wrap to next hyper-period if needed)
          |rs <h> <n> - run to state hyperperiod h and slot n (do nothing if past this state)
          |rh <n>     - run to hyper-period n (do nothing if already past the beginning of hyper-period n)
          |rt <name>  - run to slot containing thread with nickname <name>
          |i st       - show current state
          |i sc       - show schedule and current position
          |i out      - show output port values of most recently run thread
          |i in       - show input  port values of next thread to run
          |i cp <nickname> - show port values of component with given nickname
          |x          - exit
          |""".render)
  }

  def message(str: String): Unit = {
    println(str)
  }

  def interpretCmd(cmd: Command): B = {
    var done: B = false
    cmd match {
      case _: Help => {
        message(makeHelpMessage())
      }
      case Sstep(n) => {
        assert(n >= 1)
        message(s"...Stepping $n slot(s)")
        Explorer.stepSystemNSlotsIMP(n)
      }
      case Hstep(n) => {
        assert(n >= 1)
        message(s"...Stepping $n hyper-period(s)")
        if (n == 1) {
          Explorer.stepSystemOneHPIMP()
        } else if (Explorer.isHyperPeriodBoundaryH()) {
          Explorer.stepSystemNHPIMP(n)
        } else {
          message("Command not applicable: not on hyper-period boundary")
        }
      }
      case RunToHP(hpNum) => {
        assert(hpNum >= 0 & hpNum <= 1000)
        Explorer.runToHP(hpNum)
      }
      case RunToSlot(slotNum) => {
        assert(slotNum >= 0 & slotNum < Schedule.dScheduleSpec.schedule.slots.size)
        message(s"...Running to slot# $slotNum")
        Explorer.runToSlot(slotNum)
      }
      case RunToThread(threadNickName) => {
        Explorer.runToThread(threadNickName)
      }
      case RunToState(hpNum, slotNum) => {
        assert(hpNum >= 0 & hpNum <= 1000)
        assert(slotNum >= 0 & slotNum < Schedule.dScheduleSpec.schedule.slots.size)
        Explorer.runToState(hpNum, slotNum)
      }
      case RunToDomain(domainId) => {
        assert(0 <= domainId & domainId <= Schedule.dScheduleSpec.maxDomain)
        Explorer.runToDomain(domainId)
      }
      case _: Stop => done = true

      case _: Infostate => {
        val s = Explorer.scheduleState
        //Cli.showState(s, Schedule.getDomain(s), Schedule.getBridgeId(s), Schedule.threadNickName(s))
        halt("todo Infostate")
      }
      case _: Infoschedule =>
        //showSchedule(Explorer.scheduleState, Schedule.dScheduleSpec)
        halt("todo Infoschedule")

      case _: InfoInputs =>
        message(StateObserver.generatePortContentsInputsCurrent())
      case _: InfoOutputs =>
        message(StateObserver.generatePortContentsOutputsCurrent())

      case InfoComponentStateId(bridgId) =>
        message(StateObserver.generatePortContents(BridgeId.fromZ(bridgId)))
      case InfoComponentState(threadNickName) =>
        message(StateObserver.generatePortContentsByNickName(threadNickName))
      case _: InfoThreadNickNames =>
        halt("todo InfoThreadNickNames")
      //showNickNames()

      case _: Unrecognized => message("Unrecognized command")
      case _: Unsupported => message("Unsupported command")
      case _ =>
    }
    return done
  }
}
