package com.example

import akka.actor._
import akka.event.{Logging, LoggingAdapter}
import com.example._

import scala.util.Random

object ProcessManagerDriver extends CompletableApp(5) {
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

object LoanRateQuote {
  val randomLoanRateQuoteId = new Random()

  def apply(
             system: ActorSystem,
             loanRateQuoteId: String,
             taxId: String,
             amount: Integer,
             termInMonths: Integer,
             loanBroker: ActorRef): ActorRef = {
    val loanRateQuote =
      system.actorOf(
        Props(
          classOf[LoanRateQuote],
          loanRateQuoteId, taxId,
          amount, termInMonths, loanBroker),
        "loanRateQuote-" + loanRateQuote)
    loanRateQuote
  }

  def id() = {
    randomLoanRateQuoteId.nextInt(1000).toString
  }
}

class LoanRateQuote(
    loanRateQuoteId: String,
    taxId: String,
    amount: Integer,
    termInMonths: Integer,
    loanBroker: ActorRef)
  extends Actor {

  var bankLoanRateQuotes = Vector[BankLoanRateQuote]()
  var creditRatingScore: Int = _
  var expectedLoanRateQuotes: Int = _

  private def bestBankLoanRateQuote() = {
    var best = bankLoanRateQuotes(0)

    bankLoanRateQuotes map { bankLoanRateQuote =>
      if best.interestRate > bankLoanRateQuote.interestRate
      best = bankLoanRateQuote
    }
    best
  }

  private def quotableCreditScore(score: Integer): Boolean = {
    score > 399
  }

  def receive = {
    case message: StartLoanRateQuote =>
      expectedLoanRateQuotes =
        message.expectedLoanRateQuotes
      loanBroker !
        LoanRateQuoteStarted(
          loanRateQuoteId,
          taxId)

    case message: EstablishCreditScoreForLoanRateQuote =>
      creditRatingScore = message.score
      if (quotableCreditScore(creditRatingScore))
        loanBroker !
          CreditScoreForLoanRateQuoteEstablished(
            loanRateQuoteId,
            taxId,
            creditRatingScore,
            amount,
            termInMonths)
      else
        loanBroker !
          CreditScoreForLoanRateQuoteDenied(
            loanRateQuoteId,
            taxId,
            amount,
            termInMonths,
            creditRatingScore)

    case message: RecordLoanRateQuote =>
      val bankLoanRateQuote =
        BankLoanRateQuote(
          message.bankId,
          message.bankLoanRateQuoteId,
          message.interestRate)
      bankLoanRateQuotes =
        bankLoanRateQuotes :+ bankLoanRateQuote
      loanBroker !
        LoanRateQuoteRecorded(
          loanRateQuoteId,
          taxId,
          bankLoanRateQuote)

      if (bankLoanRateQuotes.size >=
        expectedLoanRateQuotes)
        loanBroker !
          LoanRateBestQuoteFilled(
            loanRateQuoteId,
            taxId,
            amount,
            termInMonths,
            creditRatingScore,
            bestBankLoanRateQuote)

    case message: TerminateLoanRateQuote =>
      loanBroker !
        LoanRateQuoteTerminated(
          loanRateQuoteId,
          taxId)
  }
}

//=========== CreditBureau

case class CheckCredit(
                        creditProcessingReferenceId: String,
                        taxId: String)

case class CreditChecked(
                          creditProcessingReferenceId: String,
                          taxId: String,
                          score: Integer)

class CreditBureau extends Actor {
  val creditRanges = Vector(300, 400, 500, 600, 700)
  val randomCreditRangeGenerator = new Random()
  val randomCreditStoreGenerator = new Random()

  def receive = {
    case message: CheckCredit =>
      val range =
        creditRanges(
          randomCreditRangeGenerator.nextInt(5)
        )
      val score =
        range
      + randomCreditRangeGenerator.nextInt(20)

      sender !
      CreditChecked(
        message.creditProcessingReferenceId,
        message.taxId,
        score
      )
  }
}

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

class Bank(
    bankId: String,
    primeRate: Double,
    ratePremium: Double)
  extends Actor {

  val randomDiscount = new Random()
  val randomQuoteId = new Random()

  private def calculateInterestRate(
                                     amount: Double,
                                     months: Double,
                                     creditScore: Double): Double = {

    val creditScoreDiscount = creditScore / 100.0 / 10.0 -
      (randomDiscount.nextInt(5) * 0.05)

    primeRate + ratePremium + ((months / 12.0) / 10.0) -
      creditScoreDiscount
  }

  def receive = {
    case message: QuoteLoanRate =>
      val interestRate =
        calculateInterestRate(
          message.amount.toDouble,
          message.termInMonths.toDouble,
          message.creditScore.toDouble)

      sender ! BankLoanRateQuoted(
        bankId, randomQuoteId.nextInt(1000).toString,
        message.loadQuoteReferenceId, message.taxId, interestRate)
  }
}