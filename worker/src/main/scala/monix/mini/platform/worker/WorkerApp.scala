package monix.mini.platform.worker

import cats.effect.{ Bracket, ExitCode, Resource }
import com.typesafe.scalalogging.LazyLogging
import monix.connect.mongodb.client.{ CollectionCodecRef, CollectionRef, MongoConnection }
import monix.eval.{ Task, TaskApp }
import monix.execution.Scheduler
import monix.mini.platform.protocol.{ Item, JoinReply, JoinResponse }
import monix.mini.platform.worker.config.WorkerConfig
import monix.mini.platform.worker.grpc.GrpcServer
import org.mongodb.scala.bson.codecs.Macros.createCodecProvider
import scala.concurrent.duration.DurationInt

object WorkerApp extends TaskApp with LazyLogging {

  implicit val config: WorkerConfig = WorkerConfig.load()

  def run(args: List[String]): Task[ExitCode] = {

    logger.info(s"Starting worker with config: $config")
    logger.info(s"Starting grpc server on endpoint: ${config.grpcServer.endPoint}")

    val itemColRef: CollectionRef[Item] = CollectionCodecRef("auction-monix-db", "item", classOf[Item], createCodecProvider[Item]())
    val connectionStr = "mongodb://localhost:27017"

    val grpcScheduler = Scheduler.io("grpc-mongo")

    MongoConnection.create1(connectionStr, itemColRef).flatMap { itemColOp =>
      Resource.make {
        Task.pure(new GrpcServer(config, itemColOp)(grpcScheduler))
      } { grpc => Task.evalAsync(grpc.shutDown()) }
    }.use { grpcServer =>
      GrpcClient(config)
        .sendJoinRequest(3, 5.seconds)
        .flatMap {
          case JoinReply(JoinResponse.JOINED, _) => Task.now(grpcServer.blockUntilShutdown()) as (ExitCode.Success)
          case JoinReply(JoinResponse.REJECTED, _) => Task.now(ExitCode.Error)
        }
    }
  }

}
