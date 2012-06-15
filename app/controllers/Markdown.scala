package controllers

import play.api._
import play.api.mvc._
import org.clapper.markwrap._

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
 
  val base = Play.current.path + "/public/markdown" 
  def load(path: String) = Action {
    val file = Play.current.getFile("public/markdown/" + path)
    
    // make sure the file is under the base, and that it exists...
    if ((file.getCanonicalPath startsWith base) && file.exists()) {
      val html = mdparser.parseToHTML(io.Source.fromFile(file))
      Ok(views.html.markdown(html))
    }
    else 
      NotFound
  }
}
