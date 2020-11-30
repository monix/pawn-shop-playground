package monix.mini.platform.http

import org.http4s.circe.jsonOf
import org.http4s.circe.jsonEncoderOf
import com.typesafe.scalalogging.LazyLogging
import io.circe.generic.auto._
import org.http4s.{HttpRoutes, Response}
import monix.eval.Task
import monix.mini.platform.master.Dispatcher
import monix.mini.platform.protocol.{Document, FindReply, InsertRequest}
import org.http4s.dsl.Http4sDsl
import scalapb_circe.JsonFormat.protoToDecoder

trait UserRoutes extends Http4sDsl[Task] with LazyLogging {

  implicit val insertRequest = jsonOf[Task, InsertRequest]

  val dispatcher: Dispatcher

  case class DocumentResponse(key: String, value: String)
  //implicit val documentEncoder = jsonEncoderOf[Task, ]

  lazy val routes: HttpRoutes[Task] = HttpRoutes.of[Task] {


    case req @ GET -> Root => {
      logger.info("Index request received")
      Ok("Monix Mini Platform")
    }

    case req @ POST -> Root / "insert" => {
      val insertOne: Task[InsertRequest] = req.as[InsertRequest]
      logger.info(s"Insert one received: $insertOne")
      Ok()
    }

    case req @ GET -> Root / "find" / key => {

      logger.info(s"Read one received request.")
      dispatcher.sendFindRequest(key).map {
        case FindReply(Some(document: Document), _) =>  Response(status = Ok).withEntity(document.toByteArray)
        case FindReply(None, _) =>  Response(status = NotFound)
      }

    }

  }

}

object UserRoutes {
  case class InsertOne(user: String, key: String, value: String)
}
