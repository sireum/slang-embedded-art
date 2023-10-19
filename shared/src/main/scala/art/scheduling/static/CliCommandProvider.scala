// #Sireum

package art.scheduling.static

import org.sireum._
import art.Art.BridgeId
import art.scheduling.static.Schedule.DScheduleSpec

@record class CliCommandProvider extends CommandProvider {
  override def nextCommand(): Command = {
    return getCommand()
  }

  // prototyping APIs that any HAMR debugging interface should support
  def message(m: String): Unit = {
    cliIO.displayOutput(m)
  }

  def formatState(scheduleState: Explorer.ScheduleState, domain: Z, bridgeId: BridgeId, threadNickName: String): String = {
    // val outString = "STATE: slot#: " + scheduleState.slotNum.toString + " ; HP#: " + scheduleState.hyperperiodNum.toString
    return s"STATE: HP#: ${scheduleState.hyperperiodNum} slot#: ${scheduleState.slotNum} domain: $domain  thread: $threadNickName ($bridgeId)"
  }

  def formatStateH(scheduleState: Explorer.ScheduleState): String = {
    val domain = Schedule.getDomainFromScheduleState(scheduleState)
    val bridgeId = Schedule.getBridgeIdFromScheduleState(scheduleState)
    val threadNickName = Schedule.getThreadNickNameFromScheduleState(scheduleState)
    return formatState(scheduleState, domain, bridgeId, threadNickName)
  }
  // The "show" methods below need to be refactored to better support MVC

  def showNickNames(): Unit = {
    cliIO.displayOutput(" Thread Nicknames")
    cliIO.displayOutput("-------------------")
    for (e <- StaticScheduler.threadNickNames.keys) {
      cliIO.displayOutput(e)
    }
  }

  def showState(scheduleState: Explorer.ScheduleState, domain: Z, bridgeId: BridgeId, threadNickName: String): Unit = {
    cliIO.displayOutput(formatState(scheduleState, domain, bridgeId, threadNickName))
  }

  def showStateH(scheduleState: Explorer.ScheduleState): Unit = {
    cliIO.displayOutput(formatStateH(scheduleState))
  }

  def showSchedule(scheduleState: Explorer.ScheduleState, dScheduleSpec: Schedule.DScheduleSpec): Unit = {
    val slots = dScheduleSpec.schedule.slots
    val hyperPeriodLength = dScheduleSpec.hyperPeriod
    val hyperPeriodNum = scheduleState.hyperperiodNum
    val stateSlotNum = scheduleState.slotNum
    val elaspedHPTicks = 0
    val remainingHPTicks = 0
    cliIO.displayOutput(s" Schedule ($hyperPeriodLength tot ticks) HP#: $hyperPeriodNum")
    cliIO.displayOutput("-------------------------------------------------")
    var slotNum: Z = 0
    for (s <- slots) {
      var prefix: String = "  "
      var suffix: String = ""
      if (slotNum == stateSlotNum) {
        val (elaspedHPTicks, remainingHPTicks) = Schedule.computeElaspedRemainingHPTicks(slotNum, dScheduleSpec)
        prefix = " *"
        suffix = s"(elasped= $elaspedHPTicks, remaining=$remainingHPTicks)"
      }
      cliIO.displayOutput(s"${prefix}$slotNum [domain=${s.domain},length=${s.length}] $suffix")
      slotNum = slotNum + 1
    }
    cliIO.displayOutput("-------------------------------------------------")
  }

  def showStep(preScheduleState: Explorer.ScheduleState,
               postScheduleState: Explorer.ScheduleState,
               dScheduleSpec: DScheduleSpec): Unit = {
    val slotNum = preScheduleState.slotNum
    val slot = dScheduleSpec.schedule.slots(slotNum)
    val domain = slot.domain
    val bridgeId = Schedule.getBridgeIdFromSlotNumber(slotNum)
    val length = slot.length
    cliIO.displayOutput("============= S t e p =============")
    cliIO.displayOutput(s"PRE-${formatState(preScheduleState, Schedule.getDomainFromScheduleState(preScheduleState), Schedule.getBridgeIdFromScheduleState(preScheduleState), Schedule.getThreadNickNameFromScheduleState(preScheduleState))}")
    cliIO.displayOutput(s"   Executing:  Domain#: $domain   Max Duration: $length")
    cliIO.displayOutput(s"POST-${formatState(postScheduleState, Schedule.getDomainFromScheduleState(postScheduleState), Schedule.getBridgeIdFromScheduleState(postScheduleState), Schedule.getThreadNickNameFromScheduleState(postScheduleState))}")
  }

  def showHyperPeriodBoundary(scheduleState: Explorer.ScheduleState): Unit = {
    cliIO.displayOutput(s"********* Hyper-Period ${scheduleState.hyperperiodNum} (beginning) **********")
  }

  def getCommand(): Command = {
    val cmdString: String = cliIO.getCommand("HAMR> ")
    val args: ISZ[String] = ops.StringOps(cmdString).split(c => c == ' ')
    val arg0: String = args(0)
    if (arg0 == "x") {
      return Stop()
    } else if (arg0 == "s") {
      var numSteps: Z = 1
      if (args.size > 1) {
        Z(args(1)) match {
          case Some(numStepsCli) => numSteps = numStepsCli
          case None() => return Unsupported()
        }
      }
      return Sstep(numSteps)
    } else if (arg0 == "help") {
      return Help()
    } else if (arg0 == "h") {
      var numSteps: Z = 1
      if (args.size > 1) {
        Z(args(1)) match {
          case Some(numStepsCli) => numSteps = numStepsCli
          case None() => return Unsupported()
        }
      }
      return Hstep(numSteps)
    } else if (arg0 == "i") {
      // need to insert a check for size greater than 1
      if (args(1) == "st") {
        return Infostate()
      } else if (args(1) == "sc") {
        return Infoschedule()
      } else if (args(1) == "out") {
        return InfoOutputs()
      } else if (args(1) == "in") {
        return InfoInputs()
      } else if (args(1) == "cpn") {
        if (args.size > 2) {
          Z(args(2)) match {
            case Some(bridgeId) => return InfoComponentStateId(bridgeId)
            case None() => return Unsupported() // expected bridgeId arg is not an integer
          }
        }
        return Unsupported() // incorrect number of arguments for "i cp" (missing bridge id arg)
      } else if (args(1) == "cp") {
        if (args.size > 2) {
          return InfoComponentState(args(2))
        }
        return Unsupported() // incorrect number of arguments for "i cp" (missing bridge id arg)
      } else if (args(1) == "nn") {
        return InfoThreadNickNames()
      } // incorrect number of arguments for "i cp" (missing bridge id arg)
      else { // ... no other info commands supported
        return Unsupported()
      }
    } else if (arg0 == "rh") {
      Z(args(1)) match {
        case Some(hpTarget) => return RunToHP(hpTarget)
        case None() => return Unsupported()
      }
    } else if (arg0 == "rd") {
      Z(args(1)) match {
        case Some(domainIdTarget) => return RunToDomain(domainIdTarget)
        case None() => return Unsupported()
      }
    } else if (arg0 == "rt") {
      val threadNickName = args(1)
      return RunToThread(threadNickName)
    } else if (arg0 == "rs") {
      if (args.size == 2) { // run to slot
        Z(args(1)) match {
          case Some(slotNumTarget) => return RunToSlot(slotNumTarget)
          case None() => return Unsupported()
        }
      } else if (args.size == 3) { // run to state
        (Z(args(1)), Z(args(2))) match {
          case (Some(hpNum), Some(slotNum)) => return RunToState(hpNum, slotNum)
          case _ => return Unsupported()
        }
      } else {
        return Unsupported()
      }
    } else {
      return Unsupported()
    }
  }
}

@ext("CliIOExt") object cliIO {
  def getCommand(prompt: String): String = $

  def displayOutput(s: String): Unit = $
}
