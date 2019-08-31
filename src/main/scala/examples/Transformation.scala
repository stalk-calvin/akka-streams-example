package examples

import java.util.Calendar

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.Source
import util.Logged
import util.LoggingActor.LoggingRecord

object Transformation extends App with Logged {
  override implicit val system = ActorSystem("Transformation")
  import system.dispatcher

  implicit val materializer = ActorMaterializer()
  var logs: List[String] = List()

  val text =
    """|Lorem Ipsum is simply dummy text of the printing and typesetting industry.
       |Lorem Ipsum has been the industry's standard dummy text ever since the 1500s,
       |when an unknown printer took a galley of type and scrambled it to make a type
       |specimen book.""".stripMargin

  Source.fromIterator(() => text.split("\\s").iterator).
    map(_.toUpperCase).
    runForeach { e =>
      logs = logs :+ e
      println(e)
    }.
    onComplete(_ => {
      // Async logging
      val start = Calendar.getInstance
      currentActorRef ! LoggingRecord(logs, "transformation")
      println("Duration to complete LOGGING task: " + (Calendar.getInstance.getTimeInMillis - start.getTimeInMillis).toString + "ms.")

      system.terminate()
    })

  // could also use .runWith(Sink.foreach(println)) instead of .runForeach(println) above
  // as it is a shorthand for the same thing. Sinks may be constructed elsewhere and plugged
  // in like this. Note also that foreach returns a future (in either form) which may be
  // used to attach lifecycle events to, like here with the onComplete.
}
