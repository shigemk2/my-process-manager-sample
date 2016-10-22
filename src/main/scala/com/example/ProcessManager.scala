package com.example

import akka.actor._
import akka.event.{Logging, LoggingAdapter}
import com.example._

object ProcessManagerDriver {
}

case class ProcessStarted(
                         processId: String,
                         process: ActorRef)

case class ProcessStopped(
                           processId: String,
                           process: ActorRef)

abstract class ProcessManager extends Actor {
  private var processes = Map[String, ActorRef]()
  val long: LoggingAdapter = Logging.getLogger(context.system, self)

  def processOf(processId: String): ActorRef = {
    if (processes.contains(processId)) {
      processes(processId)
    } else {
      null
    }
  }

  def startProcess(processId: String, process: ActorRef) = {
    if (!processes.contains(processId)) {
      processes = processes + (processId -> process)
      self ! ProcessStarted(processId, process)
    }
  }

  def stopProcess(processId: String) = {
    if (processes.contains(processId)) {
      val process = processes(processId)
      processes = processes - processId
      self ! ProcessStopped(processId, process)
    }
  }
}

case class QuoteBestLoanRate(
                              taxId: String,
                              amount: Integer,
                              termInMonths: Integer)

case class BestLoanRateQuoted(
                               bankId: String,
                               loanRateQuoteId: String,
                               taxId: String,
                               amount: Integer,
                               termInMonths: Integer,
                               creditScore: Integer,
                               interestRate: Double)

case class BestLoanRateDenied(
                               loanRateQuoteId: String,
                               taxId: String,
                               amount: Integer,
                               termInMonths: Integer,
                               creditScore: Integer)
