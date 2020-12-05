package monix.mini.platform.slave

import com.mongodb.reactivestreams.client.{MongoClient, MongoClients, MongoDatabase}
import io.lettuce.core.RedisClient
import io.lettuce.core.api.StatefulRedisConnection
import monix.mini.platform.protocol.{OperationEvent, OperationEventEntity, TransactionEventEntity}
import monix.mini.platform.slave.config.SlaveConfig
import org.bson.codecs.configuration.{CodecRegistries, CodecRegistry}
import org.bson.codecs.configuration.CodecRegistries.{fromProviders, fromRegistries}
import org.mongodb.scala.MongoClient.DEFAULT_CODEC_REGISTRY
import org.mongodb.scala.bson.codecs.Macros._

object PersistanceRepository {

  val SlaveConfig(_, _, _, mongodb, redis) = config.SlaveConfig.load()

  //mongodb
  val client: MongoClient = MongoClients.create(mongodb.url)
  val db: MongoDatabase = client.getDatabase(mongodb.database)
  val operationTypeCodecRegistry: CodecRegistry = CodecRegistries.fromCodecs(new OperationTypeCodec())
  val codecRegistry = fromRegistries(operationTypeCodecRegistry, fromProviders(classOf[OperationEventEntity], classOf[TransactionEventEntity], classOf[OperationEvent]), DEFAULT_CODEC_REGISTRY)
  val transactionsCol = db.getCollection(mongodb.transactionsCollectionName, classOf[TransactionEventEntity])
    .withCodecRegistry(codecRegistry)
  val operationsCol = db.getCollection(mongodb.operationsCollectionName, classOf[OperationEventEntity])
    .withCodecRegistry(codecRegistry)

  def interactionsKey(sender: String) = s"${redis.interactionsKeyPrefix}$sender"
  def branchesKey(branch: String) = s"${redis.branchesKeyPrefix}$branch"

  //redis
  val redisClient: RedisClient = RedisClient.create(redis.url)
  implicit val connection: StatefulRedisConnection[String, String] = redisClient.connect()

}
