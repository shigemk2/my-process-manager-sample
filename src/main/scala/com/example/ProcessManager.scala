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

//=========== LoanBroker
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

//=========== LoanRateQuote

case class StartLoanRateQuote(
                               expectedLoanRateQuotes: Integer)

case class LoanRateQuoteStarted(
                                 loanRateQuoteId: String,
                                 taxId: String)

case class TerminateLoanRateQuote()

case class LoanRateQuoteTerminated(
                                    loanRateQuoteId: String,
                                    taxId: String)

case class EstablishCreditScoreForLoanRateQuote(
                                                 loanRateQuoteId: String,
                                                 taxId: String,
                                                 score: Integer)

case class CreditScoreForLoanRateQuoteEstablished(
                                                   loanRateQuoteId: String,
                                                   taxId: String,
                                                   score: Integer,
                                                   amount: Integer,
                                                   termInMonths: Integer)

case class CreditScoreForLoanRateQuoteDenied(
                                              loanRateQuoteId: String,
                                              taxId: String,
                                              amount: Integer,
                                              termInMonths: Integer,
                                              score: Integer)

case class RecordLoanRateQuote(
                                bankId: String,
                                bankLoanRateQuoteId: String,
                                interestRate: Double)

case class LoanRateQuoteRecorded(
                                  loanRateQuoteId: String,
                                  taxId: String,
                                  bankLoanRateQuote: BankLoanRateQuote)

case class LoanRateBestQuoteFilled(
                                    loanRateQuoteId: String,
                                    taxId: String,
                                    amount: Integer,
                                    termInMonths: Integer,
                                    creditScore: Integer,
                                    bestBankLoanRateQuote: BankLoanRateQuote)

case class BankLoanRateQuote(
                              bankId: String,
                              bankLoanRateQuoteId: String,
                              interestRate: Double)

//=========== CreditBureau

case class CheckCredit(
                        creditProcessingReferenceId: String,
                        taxId: String)

case class CreditChecked(
                          creditProcessingReferenceId: String,
                          taxId: String,
                          score: Integer)

//=========== Bank

case class QuoteLoanRate(
                          loadQuoteReferenceId: String,
                          taxId: String,
                          creditScore: Integer,
                          amount: Integer,
                          termInMonths: Integer)

case class BankLoanRateQuoted(
                               bankId: String,
                               bankLoanRateQuoteId: String,
                               loadQuoteReferenceId: String,
                               taxId: String,
                               interestRate: Double)
