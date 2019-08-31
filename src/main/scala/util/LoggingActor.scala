package util

import akka.actor.{Actor, ActorLogging, Props}
import akka.event.Logging

// Async Logging
object LoggingActor {
  val props: Props = Props(new LoggingActor)

  case class LoggingRecord(result: List[String], argv: String)
}

class LoggingActor extends Actor with ActorLogging {
  import LoggingActor._

  val generalLogging = Logging(context.system, classOf[LoggingActor])

  def receive = {
    case lcr: LoggingRecord =>
      for (chunk <- lcr.result) {
        generalLogging.info(s"$chunk - ${lcr.argv}")
        Thread.sleep(100)
      }
    case msg =>
      log.warning("Got unexpected message: {}", msg)
  }
}

