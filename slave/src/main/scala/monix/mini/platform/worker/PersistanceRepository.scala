package monix.mini.platform.worker

import com.mongodb.reactivestreams.client.{MongoClient, MongoClients, MongoDatabase}
import io.lettuce.core.RedisClient
import io.lettuce.core.api.StatefulRedisConnection
import monix.mini.platform.protocol.{OperationEvent, OperationEventEntity, TransactionEventEntity}
import monix.mini.platform.worker.config.WorkerConfig
import org.bson.codecs.configuration.CodecRegistries.{fromProviders, fromRegistries}
import org.mongodb.scala.MongoClient.DEFAULT_CODEC_REGISTRY
import org.mongodb.scala.bson.codecs.Macros._

object PersistanceRepository {

  val WorkerConfig(_, _, _, _, _, mongodb, redis) = config.WorkerConfig.load()

  //mongodb
  val client: MongoClient = MongoClients.create(mongodb.url)
  val db: MongoDatabase = client.getDatabase(mongodb.database)
  val codecRegistry = fromRegistries(fromProviders(classOf[OperationEventEntity], classOf[TransactionEventEntity], classOf[OperationEvent]), DEFAULT_CODEC_REGISTRY)
  val transactionsCol = db.getCollection(mongodb.transactionsCollectionName, classOf[TransactionEventEntity])
    .withCodecRegistry(codecRegistry)
  val operationsCol = db.getCollection(mongodb.operationsCollectionName, classOf[OperationEventEntity])
    .withCodecRegistry(codecRegistry)

  //redis
  val redisClient: RedisClient = RedisClient.create(redis.url)
  implicit val connection: StatefulRedisConnection[String, String] = redisClient.connect()
  def interactionsKey(sender: String) = s"${redis.interactionsKeyPrefix}$sender"
  def branchesKey(branch: String) = s"${redis.branchesKeyPrefix}$branch"

}
