package monix.mini.platform.slave

import com.mongodb.reactivestreams.client.{MongoClient, MongoClients, MongoDatabase}
import io.lettuce.core.RedisClient
import io.lettuce.core.api.StatefulRedisConnection
import monix.mini.platform.protocol.OperationEvent
import org.bson.codecs.configuration.{CodecRegistries, CodecRegistry}
import org.bson.codecs.configuration.CodecRegistries.{fromProviders, fromRegistries}
import org.mongodb.scala.MongoClient.DEFAULT_CODEC_REGISTRY
import org.mongodb.scala.bson.codecs.Macros._

object PersistanceRepository {

  val slaveConfig = config.SlaveConfig.load()


  //mongodb
  val client: MongoClient = MongoClients.create(s"mongodb://localhost:27017")
  val db: MongoDatabase = client.getDatabase("mini-platform")
  val operationTypeCodecRegistry: CodecRegistry = CodecRegistries.fromCodecs(new OperationTypeCodec())
  val codecRegistry = fromRegistries(operationTypeCodecRegistry, fromProviders(classOf[OperationEventEntity], classOf[TransactionEventEntity], classOf[OperationEvent]), DEFAULT_CODEC_REGISTRY)
  val transactionsCol = db.getCollection("transactions", classOf[TransactionEventEntity])
    .withCodecRegistry(codecRegistry)

  val operationsCol = db.getCollection("operations", classOf[OperationEventEntity]).withCodecRegistry(codecRegistry)

  //redis
  val redisClient: RedisClient = RedisClient.create("redis://localhost:6379")
  implicit val connection: StatefulRedisConnection[String, String] = redisClient.connect()

}
