package controllers

import play.api.mvc._

import models._

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global
import play.api.libs.json.JsValue

object Application extends Controller {

  def index = Action.async { request =>
    Future { Ok(views.html.index()) }
  }

  def getTaskManagerWSConnection = WebSocket.async[JsValue] {  request =>
    TaskManager.subscribe
  }

}