package models

import dispatch._, Defaults._
import play.api.libs.json._
import play.api.libs.json.util._
import play.api.libs.json.Reads._
import play.api.libs.functional.syntax._
import play.api.libs.iteratee.{Enumerator, Iteratee, Concurrent}
import akka.util.Timeout

import akka.actor._
import scala.concurrent.duration._
import scala.language.postfixOps
import play.api.libs.concurrent.Akka
import play.api.libs.concurrent.Execution.Implicits._
import scala.concurrent.Future
import scala.util.Success
import play.api.Play.current
import akka.pattern.ask
//import dispatch.Future

object Cypher {

  /**
   * The common structure of the response
   *
   * {
   *   "columns": [ ... ],
   *   "data": [[...]]
   * }
   *
   * @param columns
   * @param data
   * @tparam A This should be a reader for the row;
   */
  case class Response[A](columns: List[String], data: List[List[A]])

  object Response {

    def reader[A](rowReader: Reads[A]) = (
      (__ \ "columns").read[List[String]] ~
      (__ \ "data").read[List[List[A]]](list(list(rowReader)))
    )(Response.apply[A] _)

  }

  /**
   *
   * @param self
   * @param data
   * @tparam A Type parameter for the data type that is stored in a node of the DB
   */
  case class Node[A](
    self: String,
    data: A
  )

  object Node {

    /**
     *
     * @param dataReader A reader for the class which is stored in the node
     * @tparam A
     * @return
     */
    def reader[A](dataReader: Reads[A]) = (
      (__ \ "self").read[String] ~
      (__ \ "data").read[A](dataReader)
    )(Node.apply[A] _)

  }

}

object TaskManager {

  case class Task(desc: String)

  implicit val taskFormat = Json.format[Task]

  // TODO Factor out into a generic data structure
  case class TaskTree(task: Task, subTasks: Vector[TaskTree] = Vector())

  val taskTreeWrites: Writes[TaskTree] = (
    (__ \ "task").write[Task] ~
    (__ \ "subTasks").lazyWrite(Writes.traversableWrites[TaskTree](taskTreeWrites))
  )(unlift(TaskTree.unapply))

  object TaskTree {

    // TODO not tail recursive; refactor
    def merge(existingTrees: Vector[TaskTree], newTrees: Vector[TaskTree]): Vector[TaskTree] = {

      newTrees.foldLeft(existingTrees) {
        case (acc, newTree) =>

          def split(processed: Vector[TaskTree], remaining: Vector[TaskTree]): (Vector[TaskTree], Option[TaskTree], Vector[TaskTree]) = {
            if (remaining.isEmpty) {
              (processed, None, remaining)
            } else if (remaining.head.task == newTree.task) {
              (processed, Some(remaining.head), remaining.tail)
            } else {
              split(processed :+ remaining.head, remaining.tail)
            }
          }

          val (left, mergeTarget, right) = split(Vector(), acc)

          mergeTarget match {
            case Some(t) => (left :+ TaskTree(t.task, merge(t.subTasks, newTree.subTasks))) ++ right
            case _       => acc :+ newTree
          }

      }

    }

    def merge(newTrees: Vector[TaskTree]): Vector[TaskTree] = merge(Vector(), newTrees)

  }


  val dbUri = "http://localhost:7474/db/data/cypher"

  val queryString =
    """
      |{
      |"query" : "start a=node({rootTask}) match p = (a)-[r:subtask*1..]->(b) where not(b-[:subtask]->()) return NODES(p)",
      |  "params" : {
      |    "rootTask" : 6
      |  }
      |}
    """.stripMargin

  val request =
    url(dbUri).
    POST.
    addHeader("Accept", "application/json; charset=UTF-8").
    setBody(queryString)

  def getTasks = Http(request) map { response =>
    val bodyAsString = response.getResponseBody("utf-8")
    val asListOfNodes = Json.parse(bodyAsString).as(Cypher.Response.reader(list(Cypher.Node.reader(taskFormat))))
    val asTasks: List[Vector[Task]] = asListOfNodes.data map { row =>
      (row map { column =>
        (column map { node =>
          node.data
        }).toVector
      }).head
    }

    val res = (asTasks map { taskList =>
      taskList.dropRight(1).foldRight(TaskTree(taskList.last)){ (prev, acc) => TaskTree(prev, Vector(acc)) }
    }).toVector

    TaskTree.merge(res)

  }

  // Actor messages
  case class  SendResponse(value: JsValue)
  case object Subscribe
  case class  Subscribed(messagesEnumerator: Enumerator[JsValue])

  implicit val timeout = Timeout(1 second)

  lazy val taskManagerActor: ActorRef = Akka.system.actorOf(Props[TaskManager])

  def subscribe: Future[(Iteratee[JsValue,_], Enumerator[JsValue])] = {

    val subscriptionResponse: Future[Any] = taskManagerActor ? Subscribe

    subscriptionResponse.map {

      case Subscribed(outgoingMessages) =>

        val incomingMessages = Iteratee.foreach[JsValue] { messageJson =>

          // parse message
          val message = messageJson.as[WebSocketMessage]

          message match {
            case WebSocketMessage(messageType) if messageType == "GetAll" =>
              getTasks.onSuccess {
                case value => taskManagerActor ! SendResponse(Json.toJson(value)(vectorTaskTreeWrites))
              }
          }

          //taskManagerActor ! SendResponse(Json.toJson(message))

        }

        (incomingMessages, outgoingMessages)

    }

  }

  val vectorTaskTreeWrites = Writes.traversableWrites[TaskTree](taskTreeWrites)

  // WebSocket messages
  case class WebSocketMessage(messageType: String)
  implicit val WebSocketMessageReads = Json.format[WebSocketMessage]


}



class TaskManager extends Actor {

  val (outgoingMessages, channel) = Concurrent.broadcast[JsValue]

  def receive = {
    case TaskManager.SendResponse(response) => channel.push(response)
    case TaskManager.Subscribe              => sender ! TaskManager.Subscribed(outgoingMessages)
  }

}


