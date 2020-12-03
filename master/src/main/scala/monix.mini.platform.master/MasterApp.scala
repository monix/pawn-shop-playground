package monix.mini.platform.master

import cats.effect.ExitCode
import com.typesafe.scalalogging.LazyLogging
import monix.eval.{ Task, TaskApp }
import org.http4s.implicits._
import org.http4s.server.blaze._
import monix.mini.platform.config.MasterConfig
import monix.mini.platform.master.http.UserRoutes
import monix.execution.Scheduler.Implicits.global

object MasterApp extends TaskApp with UserRoutes with LazyLogging {

  implicit val config: MasterConfig = MasterConfig.load()

  val dispatcher: Dispatcher = new Dispatcher()

  def run(args: List[String]): Task[ExitCode] = {
    val httpServer = BlazeServerBuilder[Task](global)
      .bindHttp(config.httpServer.port, config.httpServer.host)
      .withHttpApp(routes.orNotFound)
      .serve
      .compile
      .drain

    val grpcServer = Task.evalOnce(new GrpcServer(dispatcher).blockUntilShutdown())

    logger.info(s"Starting http server on endpoint: ${config.httpServer.endPoint}")
    logger.info(s"Starting grpc server on endpoint: ${config.grpcServer.endPoint}")

    Task
      .parSequence(Seq(httpServer, grpcServer))
      .redeem[ExitCode](ex => {
        logger.error(s"Application server error", ex)
        ExitCode.Success
      }, _ => ExitCode.Success)
  }

}
