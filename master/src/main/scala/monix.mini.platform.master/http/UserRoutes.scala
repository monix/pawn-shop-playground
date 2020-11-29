package monix.mini.platform.http

import org.http4s.circe.jsonOf
import com.typesafe.scalalogging.LazyLogging
import io.circe.generic.auto._
import org.http4s.HttpRoutes
import monix.eval.Task
import monix.mini.platform.master.Dispatcher
import monix.mini.platform.protocol.InsertRequest
import org.http4s.dsl.Http4sDsl
import scalapb_circe.JsonFormat.protoToDecoder

trait UserRoutes extends Http4sDsl[Task] with LazyLogging {

  implicit val insertOneDecoder = jsonOf[Task, InsertRequest]

  val dispatcher: Dispatcher

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
      Ok()
    }

  }

}

object UserRoutes {
  case class InsertOne(user: String, key: String, value: String)
}
