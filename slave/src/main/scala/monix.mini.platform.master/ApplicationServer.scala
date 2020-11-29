package monix.mini.platform.master

import cats.effect.ExitCode
import com.typesafe.scalalogging.LazyLogging
import monix.eval.{Task, TaskApp}
import org.http4s.implicits._
import org.http4s.server.blaze._
import monix.mini.platform.config.MasterConfig
import monix.mini.platform.http.UserRoutes
import monix.execution.Scheduler.Implicits.global
import monix.mini.platform.master.config.SlaveConfig
import monix.mini.platform.master.{Dispatcher, GrpcServer}

object ApplicationServer extends TaskApp with UserRoutes with LazyLogging {

  implicit val config: SlaveConfig = SlaveConfig.load()

  def run(args: List[String]): Task[ExitCode] = {

    val grpcServer =
    logger.info(s"Starting grpc server on endpoint: ${config.grpcServer.endPoint}")

    Task.evalOnce(new GrpcServer().blockUntilShutdown())
      .redeem[ExitCode](ex => {
        logger.error(s"Application server error", ex)
        ExitCode.Success
      }, _ => ExitCode.Success)
  }

}
