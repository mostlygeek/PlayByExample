package controllers

import akka.actor.actorRef2Scala
import akka.actor.Actor
import akka.actor.Props
import akka.pattern.ask // Implicit conversion
import akka.util.Duration
import akka.util.duration._
import akka.util.Timeout
import play.api.libs.concurrent.AkkaPromise
import play.api.mvc.Action
import play.api.mvc.Controller
import play.libs.Akka.system // Play actor system
import akka.actor.ActorRef
import org.clapper.markwrap._
import play.api.Play
import play.api.Logger

case class Error(message:String)

class HelloActor extends Actor {
  def receive = {
    case "Hello" => sender ! "Hello World"
    case _       => sender ! "What did you say?"
  }
}

case class LoadFile(path:String)

// This is the functional way of composing actors
class AlternateController extends Actor {
  
  // implicit conversion in akka.util.duration._
  implicit val timeout: Timeout = 5 seconds
  
  def receive = {
    case LoadFile(path) =>
      var controller = sender // need a permanent reference to sender
      for( // for-loop comprehension, a monadic convension
        content <- Actors.filesActorRef ? path;
        parsed  <- Actors.parseActorRef ? content
      ) yield {controller ! parsed}
    case _ => sender ! Error("Did not understand message")
  }
}

class ParseControllerActor extends Actor {
  implicit val timeout: Timeout = Timeout(Duration(5, "seconds"))
  def receive = {
    
    // Load and parse the relative path
    case path: String =>
      
      // Need an unused reference to the controller
      val controller: ActorRef = sender
      
      // Chain Actor Requests with `map`
      Actors.filesActorRef ? path map {
	    
        // Success
      	case fileContent: io.BufferedSource => Actors.parseActorRef ? fileContent map {
      	  
      	  // Success
	      case parsedContent: String => controller ! parsedContent
	      
	      // parsing Error
	      case _ => controller ! Error("Parser Returned Bad Content")
	      
	    }
	    
	    // All other cases are treated as error
	    case error:Error => controller ! error
	    case _ => controller ! Error("File Loader Returned Bad Content")
	    
      }
      
    // Message Error
    case _ => sender ! Error("Received Bad Content")
    
  }
}

class MarkdownParserActor extends Actor {
  val mdparser = MarkWrap.parserFor(MarkupType.Markdown)
  def receive = {
    case markdown: io.BufferedSource => sender ! mdparser.parseToHTML(markdown)
    case error: Error => sender ! error
    case _ => sender ! Error("Markdown Parser Did Not Understand Request")
  }
}

class FileLoadActor extends Actor {
  
  val pathPrefix = "public/markdown/"
  
  def receive = {
    
    // Load a file from a string representing the path 
  	case path:String => 
      
      val filePath = Play.current.getFile( pathPrefix + path)
      
      if (filePath.exists()) sender ! io.Source.fromFile(filePath)
      else sender ! Error("File Not Found: " + filePath)
      
    // If the message is not a string, it is an error
    case _ => sender ! Error("File Load Actor Did Not Understand Message")
  }
}

object Actors extends Controller {

  // Implicit value gobbled by `?` 
  implicit val timeout: Timeout = Timeout(Duration(5, "seconds"))

  // Append our custom actor to the play system actor tree
  val helloActorRef = system.actorOf(Props[HelloActor],           name = "hello")
  val filesActorRef = system.actorOf(Props[FileLoadActor],        name = "load")
  val parseActorRef = system.actorOf(Props[MarkdownParserActor],  name = "parse")
  val controllerRef = system.actorOf(Props[ParseControllerActor], name = "controller")
  val alternateCRef = system.actorOf(Props[AlternateController],  name = "controller2")

  def hello = Action {
    Async {
      new AkkaPromise(helloActorRef ? "Hello") map {
        case s: String => Ok(s)
        case _ => NotFound
      }
    }
  }
  
  def parse(id: String) = Action {
    Async {
      new AkkaPromise(controllerRef ? id) map {
        case s:String => Ok(views.html.markdown(s))
        case Error(s) => BadRequest(s)
        case _        => NotFound("Not Found")
      }
    }
  }
  
  def parse2(id: String) = Action {
    Async {
      new AkkaPromise(alternateCRef ? LoadFile(id)) map {
        case s: String => Ok(views.html.markdown(s))
        case Error(s)  => BadRequest(s)
        case _         => NotFound("Not Found")
      }
    }
  }

}
