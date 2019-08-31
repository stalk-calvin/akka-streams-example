package examples

import java.nio.file.Paths
import java.util.Calendar

import akka.NotUsed
import akka.actor.ActorSystem
import akka.stream.{ActorMaterializer, IOResult}
import akka.stream.scaladsl.{FileIO, Source}
import akka.util.ByteString
import util.{Logged, LoggingActor}
import util.LoggingActor.LoggingRecord

import scala.concurrent.Future

object Factorials extends App with Logged {
  override implicit val system = ActorSystem("factorials")
  implicit val materializer = ActorMaterializer()
  implicit val ec = system.dispatcher

  val source: Source[Int, NotUsed] = Source(1 to 50)
  var logs: List[String] = List()
  val factorials = source.scan(BigInt(1))((acc, next) => {
    logs = logs :+ (acc * next).toString
    acc * next
  })

  val result: Future[IOResult] =
    factorials
      .map(num => ByteString(s"$num\n"))
      .runWith(FileIO.toPath(Paths.get(getClass.getResource("/factorial.txt").getPath)))

  result.onComplete(_ => {
    // Async logging
    val start = Calendar.getInstance
    currentActorRef ! LoggingRecord(logs, "factorial")
    println("Duration to complete LOGGING task: " + (Calendar.getInstance.getTimeInMillis - start.getTimeInMillis).toString + "ms.")

    system.terminate()
  })
}
