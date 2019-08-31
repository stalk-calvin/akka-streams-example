package util

import akka.actor.ActorSystem

trait Logged {
  def system: ActorSystem
  def currentActorRef = system.actorOf(LoggingActor.props, "generalLogger")
}
