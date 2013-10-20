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
import scala.concurrent.Future

// Companion object which creates an actor and offers a subscription method (which returns a tuple of
// (incoming, outgoing) channels
object MessageBoard {

  implicit val timeout = Timeout(1 second)

  // Reference to the message board Actor instance. Remember that this is not an
  // instance itself but just a reference to it. Working with actors is possible only through messaging
  lazy val messageBoardActor: ActorRef = Akka.system.actorOf(Props[MessageBoard])

  // This method will return a tuple of iteratee and enumerator (wrapped in Future)
  // iteratee is used as incoming stream of messages from the client
  // enumerator is used as outgoing stream of messages that are sent to the client
  def subscribe: Future[(Iteratee[String,_], Enumerator[String])] = {

    // Ask for a subscription response; '?' means that we want actor to respond to our request. I responds
    // with Future[Any]
    val subscriptionResponse: Future[Any] = messageBoardActor ? Subscribe

    // Map response into a result
    subscriptionResponse.map {

      // Match against type of the response
      case Subscribed(messagesEnumerator) =>

        // Create an iteratee. This will be input stream which is returned to the client. When client
        // send messages they get pushed into this iteratee.
        // We also register a function to process the incoming messages
        val iteratee = Iteratee.foreach[String] { event =>
          messageBoardActor ! PostMessage(event)
        }

        // And return a tuple of iteratee and enumerator. This will be returned to the client.
        (iteratee, messagesEnumerator)

    }

  }

}

// Actor class itself
class MessageBoard extends Actor {

  // An enumerator and a channel; enumerator is returned to the client in the end
  val (messagesEnumerator, messagesChannel) = Concurrent.broadcast[String]

  def receive = {

    // Just push message to the channel of messages
    case PostMessage(message) => messagesChannel.push(message)

    // When receive a subscription request, respond to the sender with a Subscribed() message which contains the
    // enumerator
    case Subscribe => sender ! Subscribed(messagesEnumerator)

  }

}

// Messages used
case class PostMessage(message: String)
case object Subscribe
case class Subscribed(messagesEnumerator: Enumerator[String])

