package monix.mini.platform.http

import org.http4s.circe.jsonOf
import com.typesafe.scalalogging.LazyLogging

import io.circe.generic.auto._

import org.http4s.HttpRoutes
import monix.eval.Task
import org.http4s.dsl.Http4sDsl

trait UserRoutes extends Http4sDsl[Task] with LazyLogging  {

  implicit val insertOneDecoder = jsonOf[Task, UserRoutes.InsertOne]

  lazy val routes: HttpRoutes[Task] = HttpRoutes.of[Task] {

    case req @ GET -> Root => {
      logger.info("Index request received")
      Ok("Monix Mini Platform")
    }

   case req @ POST -> Root / "insert-one" => {
     val signUpRequest: Task[UserRoutes.InsertOne] = req.as[UserRoutes.InsertOne]
     //logger.info(s"SignUp entity received: $signUpRequest")
     Ok("Main")
   }

  }

}

object UserRoutes {
  case class InsertOne(user: String, key: String, value: String)
}
