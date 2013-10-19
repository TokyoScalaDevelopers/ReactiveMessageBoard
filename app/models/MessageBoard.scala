package models

import akka.actor._
import scala.concurrent.duration._
import scala.language.postfixOps

import play.api.libs.iteratee._
import play.api.libs.concurrent._

import akka.util.Timeout
import akka.pattern.ask

import play.api.Play.current
import play.api.libs.concurrent.Execution.Implicits._

object MessageBoard {

  implicit val timeout = Timeout(1 second)

  lazy val default = Akka.system.actorOf(Props[MessageBoard])

  def subscribe = {

    (default ? Subscribe).map {

      case Subscribed(messagesEnumerator) =>

        val iteratee = Iteratee.foreach[String] { event =>
          default ! PostMessage(event)
        }

        (iteratee, messagesEnumerator)

    }

  }

}

class MessageBoard extends Actor {

  val (messagesEnumerator, messagesChannel) = Concurrent.broadcast[String]

  def receive = {
    case PostMessage(message) => pushMessage(message)
    case Subscribe => sender ! Subscribed(messagesEnumerator)
  }

  def pushMessage(message: String) = {
    messagesChannel.push(message)
  }

}

case class PostMessage(message: String)
case object Subscribe
case class Subscribed(messagesEnumerator: Enumerator[String])

