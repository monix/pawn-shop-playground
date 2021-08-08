package monix.mini.platform.worker

import cats.effect.ExitCode
import com.typesafe.scalalogging.LazyLogging
import monix.connect.mongodb.client.{ CollectionCodecRef, CollectionRef, MongoConnection }
import monix.eval.{ Task, TaskApp }
import monix.execution.Scheduler
import monix.kafka.KafkaConsumerConfig
import monix.mini.platform.protocol.{ Buy, Item, JoinReply, JoinResponse, Pawn, Sell }
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

    val auctionDbName = "auction-monix-db"
    val itemColRef: CollectionRef[Item] = CollectionCodecRef(auctionDbName, "item", classOf[Item], createCodecProvider[Item](), protoCodecProvider)
    val buyActionsColRef: CollectionRef[Buy] = CollectionCodecRef(auctionDbName, "buy-actions", classOf[Buy], createCodecProvider[Buy](), protoCodecProvider)
    val sellActionsColRef: CollectionRef[Sell] = CollectionCodecRef(auctionDbName, "sell-actions", classOf[Sell], createCodecProvider[Sell](), protoCodecProvider)
    val pawnActionsColRef: CollectionRef[Pawn] = CollectionCodecRef(auctionDbName, "pawn-actions", classOf[Pawn], createCodecProvider[Pawn](), protoCodecProvider)

    val connectionStr = "mongodb://localhost:27017"

    val grpcScheduler = Scheduler.io("grpc-mongo")

    implicit val kafkaConsumerConfig = KafkaConsumerConfig.load().copy(groupId = "some-group-id")
    implicit val item = Item
    val itemsKafkaConsumer = new KafkaProtoConsumer[Item]("items")
    val buyActionsKafkaConsumer = new KafkaProtoConsumer[Buy]("buy-actions")
    val sellActionsKafkaConsumer = new KafkaProtoConsumer[Sell]("sell-actions")
    val pawnActionsKafkaConsumer = new KafkaProtoConsumer[Pawn]("pawn-actions")

    MongoConnection.create4(connectionStr, (itemColRef, buyActionsColRef, sellActionsColRef, pawnActionsColRef)).use {
      case (itemsOp, buyActionsOp, sellActionsOp, pawnActionsOp) =>
        val grpcServer = new GrpcServer(config, itemsOp, buyActionsOp, sellActionsOp, pawnActionsOp)(grpcScheduler)
        GrpcClient(config)
          .sendJoinRequest(3, 5.seconds)
          .flatMap {
            case JoinReply(JoinResponse.JOINED, _) => {
              Task.raceMany(Seq(
                Task.evalAsync(grpcServer.blockUntilShutdown()),
                WorkerFlow(itemsKafkaConsumer, itemsOp.single).run().completedL,
                WorkerFlow(buyActionsKafkaConsumer, buyActionsOp.single).run().completedL,
                WorkerFlow(sellActionsKafkaConsumer, sellActionsOp.single).run().completedL,
                WorkerFlow(pawnActionsKafkaConsumer, pawnActionsOp.single).run().completedL)).guarantee(Task(grpcServer.shutDown()))
                .as(ExitCode.Success)
            }
            case JoinReply(JoinResponse.REJECTED, _) => Task.now(ExitCode.Error)
          }
    }
  }

}
