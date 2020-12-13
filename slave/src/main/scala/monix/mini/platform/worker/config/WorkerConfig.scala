package monix.mini.platform.worker.config

import WorkerConfig.{ GrpcServerConfig, MongoDbConfig, RedisConfig }
import pureconfig._
import pureconfig.generic.ProductHint
import pureconfig.generic.auto._

import scala.concurrent.duration.FiniteDuration

case class WorkerConfig(slaveId: String, grpcTimeout: FiniteDuration, joinRequestRetries: Int, grpcServer: GrpcServerConfig, dispatcherServer: GrpcServerConfig, mongodb: MongoDbConfig, redis: RedisConfig)

object WorkerConfig {

  implicit val confHint: ProductHint[WorkerConfig] = ProductHint[WorkerConfig](ConfigFieldMapping(CamelCase, KebabCase))

  def load(): WorkerConfig = loadConfigOrThrow[WorkerConfig]

  case class GrpcServerConfig(
    host: String,
    port: Int,
    endPoint: String)

  case class MongoDbConfig(
    host: String,
    port: Int,
    url: String,
    database: String,
    transactionsCollectionName: String,
    operationsCollectionName: String)

  case class RedisConfig(
    host: String,
    port: Int,
    url: String,
    interactionsKeyPrefix: String,
    branchesKeyPrefix: String,
    fraudstersKey: String)

}

