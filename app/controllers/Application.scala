package controllers

import play.api._
import play.api.mvc._
import play.api.libs.iteratee._
import play.api.libs.json._

import models._

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

object Application extends Controller {

  def index = Action.async { request =>
    Future { Ok(views.html.index("Your new application is ready")) }
  }

  def messageBoard = WebSocket.async[String] {  request =>
    MessageBoard.subscribe
  }

}