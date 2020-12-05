package monix.mini.platform.slave.config

import monix.mini.platform.slave.config.SlaveConfig.{GrpcServerConfig, MongoDbConfig, RedisConfig}
import pureconfig._
import pureconfig.generic.ProductHint
import pureconfig.generic.auto._

case class SlaveConfig(slaveId: String, grpcServer: GrpcServerConfig, masterServer: GrpcServerConfig, mongodb: MongoDbConfig, redis: RedisConfig)

object SlaveConfig {

  implicit val confHint: ProductHint[SlaveConfig] = ProductHint[SlaveConfig](ConfigFieldMapping(CamelCase, KebabCase))

  def load(): SlaveConfig = loadConfigOrThrow[SlaveConfig]

  case class GrpcServerConfig(host: String,
                              port: Int,
                              endPoint: String)

  case class MongoDbConfig(host: String,
                           port: Int,
                           url: String,
                           database: String,
                           transactionsCollectionName: String,
                           operationsCollectionName: String)

  case class RedisConfig(host: String,
                         port: Int,
                         url: String,
                         interactionsKeyPrefix: String,
                         branchesKeyPrefix: String)

}

