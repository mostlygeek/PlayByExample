package controllers

import play.api._
import play.api.mvc._
import org.clapper.markwrap._
import java.io.{File}
import play.api.libs.concurrent._
import play.api.Play.current

/**
 * this controller allows us to: 
 *
 *   - manage documentation in Markdown format
 *   - automatically convert it to HTML 
 *   - feel awesome.
 *
 * Design: 
 *   - all markdown files (*.md) are stored in app/public/markdown/somefile.md
 *   - this controller *only* looks in that directory for the content
 */
object Markdown extends Controller {
  //ref: http://software.clapper.org/markwrap/#parsing_markdown 
  private val mdparser = MarkWrap.parserFor(MarkupType.Markdown)
  private val base = Play.current.getFile("/public/markdown").getCanonicalPath

  /**
   * Does a synchronous load of the markdown source. This blocks
   * the execution read, in effect, blocking answering of all requests
   * while IO is loading
   *
   */
  def loadSynchronous(path: String) = Action {
    loadAndParse(path) match {
      case Some(html) => Ok(views.html.markdown(html))
      case _ => NotFound
    }
  }
 
  /**
   * This works precisely like the synchronous load except it does
   * the work within an Akka actor and does not block the main 
   * thread from handling additional requests
   *
   * Ref: http://www.playframework.org/documentation/2.0.1/ScalaAsync
   */ 
  def loadAsync(path: String) = Action {
    Async {
      val promise: Promise[Option[String]] = Akka.future {
        loadAndParse(path)
      }

      promise.map( i => i match {
          case Some(html) => Ok(views.html.markdown(html))
          case _ => NotFound
      })
    } 
  }

  private def loadAndParse(path: String): Option[String] = {
    val file = Play.current.getFile("public/markdown/" + path)
    if ((file.getCanonicalPath startsWith base) && file.exists())
      Some(mdparser.parseToHTML(io.Source.fromFile(file)))
    else 
      None
  }
}
