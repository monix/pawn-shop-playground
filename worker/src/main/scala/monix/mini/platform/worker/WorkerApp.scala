package monix.mini.platform.worker

import cats.effect.ExitCode
import com.typesafe.scalalogging.LazyLogging
import monix.connect.mongodb.client.{CollectionCodecRef, CollectionRef, MongoConnection}
import monix.eval.{Task, TaskApp}
import monix.execution.Scheduler
import monix.kafka.KafkaConsumerConfig
import monix.mini.platform.protocol.{Item, JoinReply, JoinResponse}
import monix.mini.platform.worker.mongo.Codecs.protoCodecProvider
import monix.mini.platform.worker.config.WorkerConfig
import monix.mini.platform.worker.grpc.GrpcServer
import org.mongodb.scala.bson.codecs.Macros.createCodecProvider

import scala.concurrent.duration.DurationInt

object WorkerApp extends TaskApp with LazyLogging {

  implicit val config: WorkerConfig = WorkerConfig.load()

  def run(args: List[String]): Task[ExitCode] = {

    logger.info(s"Starting worker with config: $config")
    logger.info(s"Starting grpc server on endpoint: ${config.grpcServer.endPoint}")

    val itemColRef: CollectionRef[Item] = CollectionCodecRef("auction-monix-db", "item", classOf[Item], createCodecProvider[Item](), protoCodecProvider)

    val connectionStr = "mongodb://localhost:27017"

    val grpcScheduler = Scheduler.io("grpc-mongo")

    implicit val kafkaConsumerConfig = KafkaConsumerConfig.load().copy(groupId = "some-group-id")
    implicit val item = Item
    val itemsKafkaConsumer = new KafkaProtoConsumer[Item]("items")

    MongoConnection.create1(connectionStr,  itemColRef).use { itemColOp =>

      val grpcServer = new GrpcServer(config, itemColOp)(grpcScheduler)
      GrpcClient(config)
        .sendJoinRequest(3, 5.seconds)
        .flatMap {
          case JoinReply(JoinResponse.JOINED, _) => {
            Task.race(
              Task.evalAsync(grpcServer.blockUntilShutdown()),
              WorkerFlow(itemsKafkaConsumer, itemColOp.single).run().completedL
            ).guarantee(Task(grpcServer.shutDown()))
              .as(ExitCode.Success)
          }
          case JoinReply(JoinResponse.REJECTED, _) => Task.now(ExitCode.Error)
        }
    }
  }

}
