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

// We use case classes to send typed messages
case class LoadFile(path:String)

// This is the functional way of composing actors
class AlternateController extends Actor {
  
  // implicit conversion in akka.util.duration._
  implicit val timeout: Timeout = 5 seconds
  
  def receive = {
    
    // The LoadFile object tells AlternateController to
    // load and parse the file specified by `path`
    case LoadFile(query) =>
      var controller = sender // need a permanent reference to sender
      
      // Pipe the output of `filesActorRef` into `parseActorRef`
      for( // for-loop comprehension, a monadic convension
        matches <- Actors.locatorRef ? query;
        path    <- Actors.pathRanker ? matches;
        content <- Actors.filesActorRef ? path;
        parsed  <- Actors.parseActorRef ? content
      ) yield {controller ! parsed}
    
    // We only recognize LoadFile, all other typed messages return an error
    case _ => sender ! Error("Did not understand message")
  }
}

// This does the same as the AlternateController class
// but is more verbose because it doesn't use for-loop comprehension
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

// Parse a BufferedSource object containing markdown code
// Return an HTML formatted string 
class MarkdownParserActor extends Actor {
  val mdparser = MarkWrap.parserFor(MarkupType.Markdown)
  def receive = {
    
    // Parse a BufferedSource object
    case markdown: io.BufferedSource => sender ! mdparser.parseToHTML(markdown)
    
    // If an error has been piped from another object, propagate it
    // A cool error feature would be to build a stack-trace of all the actors
    // the error has been received by.
    case error: Error => sender ! error
    
    // Create a new error in all other cases
    case _ => sender ! Error("Markdown Parser Did Not Understand Request")
  }
}

class PathRankerActor extends Actor {
  def receive = {
    case Nil => sender ! Error("List of possible paths is empty")
    case list: List[_] =>
      sender ! list(0)
    case error:Error => sender ! error
    case _ => sender ! Error("Path Ranker Did Not Understand the Message")
  }
}

// Give it a path, string, or name
// and it returns the associated path
class FileLocatorActor extends Actor {
  val suffixes = List("",".md")
  val pathjail = "public/markdown/"
  def receive = {
    case query:String => sender ! find(query)
    case _            => sender ! Error("Query Type Not Understood")
  }
  def find(query:String) = {
    
    // Convert queries to a list of full path
    // prefixed with pathjail
    // and suffixed by suffixes
    val files = suffixes map ( pathjail + query + _ ) map ( Play.current.getFile( _ ) )
    
    // Return all full paths that exist
    for( file <- files if file.exists() ) yield file
    
  }
}

// Loads files from the local file system
class FileLoadActor extends Actor {
  
  val pathPrefix = "public/markdown/"
  
  def receive = {
    
    // Accept a file pointer
    // Simply read and return the contents as a BufferedSource
    case file: java.io.File => 
      sender ! io.Source.fromFile(file)
      
    // Load a file from a string representing the path
    // The file path will be jailed by `pathPrefix`
    case path:String => 
      
      // I think this appends the current play root directory
      val filePath = Play.current.getFile( pathPrefix + path)
      
      if (filePath.exists()) sender ! io.Source.fromFile(filePath)
      else sender ! Error("File Not Found: " + filePath)
      
    
    // Propagate existing errors
    case error: Error => sender ! error
    
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
  val locatorRef    = system.actorOf(Props[FileLocatorActor])
  val pathRanker    = system.actorOf(Props[PathRankerActor])
  
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
