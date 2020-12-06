package monix.mini.platform.feeder

import cats.effect.ExitCode
import com.typesafe.scalalogging.LazyLogging
import monix.eval.{Task, TaskApp}
import monix.execution.Scheduler.Implicits.global
import monix.mini.platform.protocol.{JoinReply, JoinResponse}
import monix.mini.platform.feeder.config.SlaveConfig

object SlaveApp extends TaskApp with LazyLogging {

  implicit val config: SlaveConfig = SlaveConfig.load()

  def run(args: List[String]): Task[ExitCode] = {

    logger.info(s"Starting grpc server on endpoint: ${config.grpcServer.endPoint}")

    GrpcClient
      .sendJoinRequest
      .flatMap {
        case JoinReply(JoinResponse.JOINED, _) => Task.now(new GrpcServer().blockUntilShutdown()).as(ExitCode.Success)
        case JoinReply(JoinResponse.REJECTED, _) => Task.now(ExitCode.Error)
      }
  }

}
