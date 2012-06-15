package controllers

import play.api._
import play.api.mvc._
import models._
import com.mongodb.casbah.Imports._

/**
 * Just some stuff to play around w/ casbah and mongodb
 *
 * Check project/Build.scala to see that salat + casbah has been
 * added as a dependency. Running: > play compile will suck in all 
 * the necessary libraries to make the mongo driver work...
 */
object Mongo extends Controller {

  val col = MongoConnection()("testDb")("testCol")
  val _id:java.lang.Integer = 1

  def index() = Action {
    Ok("index")
  }
 
  def list() = Action {
    Ok("list goes here")
  }
  
  /**
   * looks for a document within mongo, if it can't find it
   * it will create it and tell the user to reload the page.
   */
  def create(id: String) = Action {
    col.findOneByID(id)  match { 
      case None => { 
          // the += operator allows Docs to be added to mongo easily 
          col += MongoDBObject("_id" -> id, "created" -> true)
          Ok("created: " + id + ", refresh to load record")
      }
      case _ => Ok("already exists") 
    } 
  }

  def show(id: String) = Action {
    col.findOneByID(id) match {
      case None => Ok("Not, Found")
      case result => Ok("Found: " + result)
    } 
  }
}
