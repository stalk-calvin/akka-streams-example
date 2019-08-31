package examples

import akka.NotUsed
import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.{Flow, Sink, Source}

object FlowSink extends App {
  implicit val system = ActorSystem()
  implicit val mat = ActorMaterializer()

  val source: Source[Int, NotUsed] = Source(0 to 200000)

  val flow = Flow[Int].map(_.toString())

  val sink = Sink.foreach[String](println(_))

  val runnable = source.via(flow).to(sink)

  runnable.run()
}
