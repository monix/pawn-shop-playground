package monix.mini.platform.master

import cats.effect.ExitCode
import com.typesafe.scalalogging.LazyLogging
import monix.eval.{ Task, TaskApp }
import monix.execution.ExecutionModel
import org.http4s.implicits._
import org.http4s.server.blaze._
import monix.mini.platform.config.MasterConfig
import monix.mini.platform.master.http.UserRoutes
import monix.execution.Scheduler.Implicits.global

import scala.concurrent.duration._

object MasterApp extends TaskApp with UserRoutes with LazyLogging {

  implicit val config: MasterConfig = MasterConfig.load()

  logger.info(s"Starting master server with config: $config")

  val dispatcher: Dispatcher = new Dispatcher()

  def run(args: List[String]): Task[ExitCode] = {

    global.scheduleWithFixedDelay(5.seconds, 15.seconds)(logger.info("Still alive."))

    logger.info(s"Starting http server on endpoint: ${config.httpServer.endPoint}")
    val httpIo = monix.execution.Scheduler.fixedPool("http-io", 2)
    val httpServer = BlazeServerBuilder[Task](httpIo)
      .bindHttp(config.httpServer.port, config.httpServer.host)
      .withHttpApp(routes.orNotFound)
      .serve
      .compile
      .drain
      .executeOn(httpIo)

    val grpcIo = monix.execution.Scheduler.fixedPool("grpc-io", 1)
    val grpcServer = Task(new GrpcServer(dispatcher).blockUntilShutdown()).executeOn(grpcIo)

    Task.parSequence(Seq(httpServer, grpcServer))
      .redeem[ExitCode](ex => {
        logger.error(s"Application server error", ex)
        ExitCode.Success
      }, _ => {
        logger.info("The application finished gracefully.")
        ExitCode.Success
      })
  }

}
