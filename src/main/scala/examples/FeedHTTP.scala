package examples

import akka.pattern.after
import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.ws.{BinaryMessage, Message, TextMessage}
import akka.http.scaladsl.server.Directives._
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.{Flow, Sink, Source}

import scala.collection.Seq
import scala.concurrent.{ExecutionContext, Future}
import scala.concurrent.duration._
import scala.util.{Failure, Random, Success}

object FeedHTTP extends App {
  implicit val system = ActorSystem()
  implicit val mat = ActorMaterializer()

  implicit val ec: ExecutionContext = system.dispatcher

  case class PublishResult(result: Seq[String])

  object Database {
    def asyncBulkInsert(entries: Seq[String])(implicit system: ActorSystem): Future[PublishResult] = {
      // simulate that writing to a database takes ~30 millis
      //      after(30.millis, system.scheduler, system.dispatcher, Future.successful(entries))
      after(30.millis, system.scheduler) {
        Future(PublishResult(entries))(system.dispatcher)
      } (system.dispatcher)
    }
  }


  def greeter: Flow[Message, Message, Any] =
    Flow[Message].mapConcat {
      case tm: TextMessage =>
        TextMessage(Source.single("Hello ") ++ tm.textStream ++ Source.single("!")) :: Nil
      case bm: BinaryMessage =>
        // ignore binary messages but drain content to avoid the stream being clogged
        bm.dataStream.runWith(Sink.ignore)
        Nil
    }

  val measurementsFlow =
    Flow[Message].flatMapConcat { message =>
      // handles both strict and streamed ws messages by folding
      // the later into a single string (in memory)
      message.asTextMessage.getStreamedText.fold("")(_ + _)
    }
      .groupedWithin(1000, 1.second)
      .mapAsync(5)(Database.asyncBulkInsert)
      .map(written => TextMessage("wrote up to: " + written.result.last))

  val route =
    concat(
      path("measurements") {
        get {
          handleWebSocketMessages(measurementsFlow)
        }
      },
      path("greeter") {
        get {
          handleWebSocketMessages(greeter)
        }
      }
    )


  val futureBinding = Http().bindAndHandle(route, "127.0.0.1", 8090)

  futureBinding.onComplete {
    case Success(binding) =>
      val address = binding.localAddress
      println(s"Akka HTTP server running at ${address.getHostString}:${address.getPort}")

    case Failure(ex) =>
      println(s"Failed to bind HTTP server: ${ex.getMessage}")
      ex.fillInStackTrace()

  }

}