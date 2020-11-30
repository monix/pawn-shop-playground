package monix.mini.platform.master

import cats.effect.ExitCode
import com.mongodb.reactivestreams.client.{ MongoClient, MongoClients, MongoCollection, MongoDatabase }
import com.typesafe.scalalogging.LazyLogging
import io.grpc.ManagedChannelBuilder
import monix.eval.{ Task, TaskApp }
import monix.execution.Scheduler.Implicits.global
import monix.mini.platform.master.config.SlaveConfig
import monix.mini.platform.protocol.{ Document, JoinReply, JoinResponse }
import monix.mini.platform.slave.GrpcServer
import org.mongodb.scala.MongoClient.DEFAULT_CODEC_REGISTRY
import org.mongodb.scala.bson.codecs.Macros._
import org.bson.codecs.configuration.CodecRegistries.{ fromProviders, fromRegistries }

object SlaveApp extends TaskApp with LazyLogging {

  implicit val config: SlaveConfig = SlaveConfig.load()

  def run(args: List[String]): Task[ExitCode] = {

    logger.info(s"Starting grpc server on endpoint: ${config.grpcServer.endPoint}")

    //mongodb
    val client: MongoClient = MongoClients.create(s"mongodb://localhost:27017")
    val db: MongoDatabase = client.getDatabase("mydb")
    val codecRegistry = fromRegistries(fromProviders(classOf[Document]), DEFAULT_CODEC_REGISTRY)
    val col: MongoCollection[Document] = db.getCollection("monix-mini-platform", classOf[Document])
      .withCodecRegistry(codecRegistry)

    GrpcClient
      .sendJoinRequest
      .flatMap {
        case JoinReply(JoinResponse.JOINED, _) => {
          Task.now(new GrpcServer(col).blockUntilShutdown()).as(ExitCode.Success)
        }
        case JoinReply(JoinResponse.REJECTED, _) => Task.now(ExitCode.Error)
      }
  }

}
