package models

import dispatch._, Defaults._
import play.api.libs.json._
import play.api.libs.json.util._
import play.api.libs.json.Reads._
import play.api.libs.functional.syntax._

object Cypher {

  case class Task(desc: String)

  object Task {
    implicit val reader = Json.reads[Task]
}

/*  implicit val */

  case class Response[A](columns: List[String], data: List[List[List[Node[A]]]])

  //implicit val ln = Json.reads[List[Node]]

  object Response {

    implicit val reader = (
      (__ \ "columns").read[List[String]] ~
      (__ \ "data").read[List[List[List[Node[Task]]]]](list(list(list(Node.reader(Task.reader)))))
    )(Response.apply[Task] _)

//    type Data = List[Row]

//    type Row = List[String]

  }

  case class Node[A](
    self: String,
    data: A
  )

  object Node {
    implicit def reader[A](dataReader: Reads[A]) = (
      (__ \ "self").read[String] ~
      (__ \ "data").read[A](dataReader)
    )(Node.apply[A] _)
  }



/*  "extensions": {},
  "paged_traverse": "http://localhost:7474/db/data/node/6/paged/traverse/{returnType}{?pageSize,leaseTime}",
  "labels": "http://localhost:7474/db/data/node/6/labels",
  "outgoing_relationships": "http://localhost:7474/db/data/node/6/relationships/out",
  "traverse": "http://localhost:7474/db/data/node/6/traverse/{returnType}",
  "all_typed_relationships": "http://localhost:7474/db/data/node/6/relationships/all/{-list|&|types}",
  "property": "http://localhost:7474/db/data/node/6/properties/{key}",
  "all_relationships": "http://localhost:7474/db/data/node/6/relationships/all",
  "self": "http://localhost:7474/db/data/node/6",
  "properties": "http://localhost:7474/db/data/node/6/properties",
  "outgoing_typed_relationships": "http://localhost:7474/db/data/node/6/relationships/out/{-list|&|types}",
  "incoming_relationships": "http://localhost:7474/db/data/node/6/relationships/in",
  "incoming_typed_relationships": "http://localhost:7474/db/data/node/6/relationships/in/{-list|&|types}",
  "create_relationship": "http://localhost:7474/db/data/node/6/relationships",
  "data": {
    "desc": "Roll a party"
  }*/

}

object TaskManager {

  //import Cypher._

  //type CypherResponseData = List[String]

  //case class CypherResponse(columns: List[String], data: CypherResponseData)
  //List[List[String]]
  //case class CypherResponseData()

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
    Json.parse(bodyAsString).as[Cypher.Response[Cypher.Task]]
  }

}
