package controllers

import akka.actor.actorRef2Scala
import akka.actor.Actor
import akka.actor.Props
import akka.pattern.ask // Implicit conversion
import akka.util.Duration
import akka.util.Timeout
import play.api.libs.concurrent.AkkaPromise
import play.api.mvc.Action
import play.api.mvc.Controller
import play.libs.Akka.system // Play actor system

class HelloActor extends Actor {
  def receive = {
    case "Hello" => sender ! "Hello World"
    case _       => sender ! "What did you say?"
  }
}

object Actors extends Controller {

  // Implicit value gobbled by `?` 
  implicit val timeout: Timeout = Timeout(Duration(5, "seconds"))

  // Append our custom actor to the play system actor tree
  val helloActorRef = system.actorOf(Props[HelloActor], name = "hello")

  def hello = Action {
    Async {
      new AkkaPromise(helloActorRef ? "Hello") map {
        case s: String => Ok(s)
        case _ => NotFound
      }
    }
  }

}
