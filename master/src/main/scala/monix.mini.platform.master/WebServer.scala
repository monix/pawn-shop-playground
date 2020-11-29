package monix.mini.platform

import cats.implicits._
import org.http4s.implicits._
import org.http4s.server.blaze._
import cats.implicits._
import cats.effect.ExitCode
import com.typesafe.scalalogging.LazyLogging
import monix.eval.{Task, TaskApp}
import org.http4s.dsl.io._
import org.http4s.implicits._
import org.http4s.server.blaze._
import monix.execution.Scheduler.Implicits.global
import monix.mini.platform.config.MasterConfig
import monix.mini.platform.http.UserRoutes

object WebServer extends TaskApp with UserRoutes with LazyLogging {

  val config: MasterConfig = MasterConfig.load()
  val host = config.server.host
  val port = config.server.port

  val endPoint = config.server.endPoint

  logger.info(s"Starting web server on endpoint: $endPoint")

  def run(args: List[String]): Task[ExitCode] =
    BlazeServerBuilder[Task]
      .bindHttp(port, host)
      .withHttpApp(routes.orNotFound)
      .serve
      .compile
      .drain
      .as(ExitCode.Success)

}
