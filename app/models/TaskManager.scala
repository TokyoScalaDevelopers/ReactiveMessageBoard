package models

import dispatch._, Defaults._
import play.api.libs.json._
import play.api.libs.json.util._
import play.api.libs.json.Reads._
import play.api.libs.functional.syntax._

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

  case class TaskTree(task: Task, subTasks: Vector[TaskTree] = Vector()) {

//    def merge(that: TaskTree): List[TaskTree] = {
//
//      if (task == that.task) {
//        // go deeper
//      } else {
//        // add the branch
//
//      }
//
//    }

  }

  object TaskTree {

    def merge(existingTrees: Vector[TaskTree], newTree: TaskTree): Vector[TaskTree] = {

      //val sameTree = existingTrees.find(_ == newTree)

      val (rest, target) = existingTrees.foldLeft((Vector(): Vector[TaskTree], None: Option[TaskTree])) {
        case ((skipped, found), value) =>
          if (value.task == newTree.task) {
            (skipped, Some(value))
          } else {
            (skipped :+ value, found)
          }
      }

      target match {
        // TODO now it works with singleton lists only, and it also looses ordering
        case Some(t) =>
          //if (!newTree.subTasks.isEmpty) {
            rest :+ TaskTree(t.task, merge(t.subTasks, newTree.subTasks.head))
          //} else {
            //rest :+ TaskTree(t.task, t.subTasks)
          //}
        case _ => existingTrees :+ newTree
      }

//      if (existingTrees.contains(newTree)) {
//        // go deeper
//      } else {
//
//      }

    }

    //def merge(existingTrees: TaskTree

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
    val asListOfNodes = Json.parse(bodyAsString).as(Cypher.Response.reader(list(Cypher.Node.reader(Json.reads[Task]))))
    val asTasks: List[Vector[Task]] = asListOfNodes.data map { row =>
      (row map { column =>
        (column map { node =>
          node.data
        }).toVector
      }).head
    }

    val root = TaskTree(Task("root"))

    val res = asTasks map { taskList =>
      //TODO Fold right on the linked lists is a big no-no. Refactor.
      taskList.dropRight(1).foldRight(TaskTree(taskList.last)){ (prev, acc) => TaskTree(prev, Vector(acc)) }
    }

    //res.toVector

    TaskTree.merge(Vector(res.head),res(1))

  }

}
