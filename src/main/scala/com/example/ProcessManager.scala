package com.example

import akka.actor._
import com.example._

object ProcessManagerDriver {
}

case class ProcessStarted(
                         processId: String,
                         process: ActorRef)

case class ProcessStopped(
                           processId: String,
                           process: ActorRef)
