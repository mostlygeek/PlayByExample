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
 *   - all markdown files (*.md) are stored in app/content
 *   - this controller *only* looks in app/content like a chroot
 *   - 
 */
object Markdown extends Controller {
  //ref: http://software.clapper.org/markwrap/#parsing_markdown 
  private lazy val mdparser = MarkWrap.parserFor(MarkupType.Markdown)
  
  def load(path: String) = Action {
    Ok("Requested: " + path)
  }
}
