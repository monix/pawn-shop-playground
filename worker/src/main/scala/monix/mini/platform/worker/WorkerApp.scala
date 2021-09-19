package monix.mini.platform.worker

import cats.effect.ExitCode
import com.typesafe.scalalogging.LazyLogging
import monix.connect.mongodb.client.{CollectionCodecRef, MongoConnection}
import monix.eval.{Task, TaskApp}
import monix.execution.Scheduler
import monix.kafka.KafkaConsumerConfig
import monix.mini.platform.protocol.{Buy, Item, JoinReply, JoinResponse, Pawn, Sell}
import monix.mini.platform.worker.mongo.Codecs.protoCodecProvider
import monix.mini.platform.worker.config.WorkerConfig
import monix.mini.platform.worker.grpc.GrpcServer
import org.mongodb.scala.bson.codecs.Macros.createCodecProvider

import scala.concurrent.duration.DurationInt

object WorkerApp extends TaskApp with LazyLogging {

  val grpcIo = Scheduler.io("grpc-mongo")
  def run(args: List[String]): Task[ExitCode] = {

    Task.fromEither(WorkerConfig.load()).memoizeOnSuccess.flatMap{ case  WorkerConfig(slaveId, grpcConfig, mongoConfig, redisConfig, kafkaConfig) =>
    val itemColRef = CollectionCodecRef(mongoConfig.database, mongoConfig.itemsColName, classOf[Item], createCodecProvider[Item](), protoCodecProvider)
    val buyActionsColRef =  CollectionCodecRef(mongoConfig.database, mongoConfig.buyActionsColName, classOf[Buy], createCodecProvider[Buy](), protoCodecProvider)
    val sellActionsColRef = CollectionCodecRef(mongoConfig.database, mongoConfig.sellActionColName, classOf[Sell], createCodecProvider[Sell](), protoCodecProvider)
    val pawnActionsColRef = CollectionCodecRef(mongoConfig.database, mongoConfig.pawnActionsColName, classOf[Pawn], createCodecProvider[Pawn](), protoCodecProvider)

      implicit val consumerConfig = KafkaConsumerConfig.load().copy(groupId = kafkaConfig.groupId)

      val itemsConsumer = KafkaProtoConsumer[Item](kafkaConfig.itemsTopic)
      val buyActionsConsumer = KafkaProtoConsumer[Buy](kafkaConfig.buyEventsTopic)
      val sellActionsConsumer = KafkaProtoConsumer[Sell](kafkaConfig.sellEventsTopic)
      val pawnActionsConsumer = KafkaProtoConsumer[Pawn](kafkaConfig.pawnEventsTopic)

      for {
        exitCode <- MongoConnection.create4(mongoConfig.url, (itemColRef, buyActionsColRef, sellActionsColRef, pawnActionsColRef)).use {
        case (itemsOp, buyActionsOp, sellActionsOp, pawnActionsOp) =>
          logger.info(s"Starting worker server with config: $grpcConfig")
          val grpcServer = new GrpcServer(grpcConfig, itemsOp, buyActionsOp, sellActionsOp, pawnActionsOp)(grpcIo)
          GrpcClient(slaveId, grpcConfig)
            .sendJoinRequest(3, 5.seconds)
            .flatMap {
              case JoinReply(JoinResponse.JOINED, _) => {
                Task.raceMany(Seq(
                  Task.evalAsync(grpcServer.blockUntilShutdown()),
                  InboundFlow(itemsConsumer, itemsOp.single).run().completedL,
                  InboundFlow(buyActionsConsumer, buyActionsOp.single).run().completedL,
                  InboundFlow(sellActionsConsumer, sellActionsOp.single).run().completedL,
                  InboundFlow(pawnActionsConsumer, pawnActionsOp.single).run().completedL)).guarantee(Task(grpcServer.shutDown()))
                  .as(ExitCode.Success)
              }
              case JoinReply(JoinResponse.REJECTED, _) => Task.now(ExitCode.Error)
            }
      }
    } yield exitCode
    }


}
