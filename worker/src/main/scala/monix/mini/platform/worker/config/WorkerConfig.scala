package monix.mini.platform.worker.config

import WorkerConfig.{GrpcConfig, GrpcEndpoint, KafkaConfiguration, MongoDbConfig, RedisConfig}
import monix.kafka.KafkaConsumerConfig
import pureconfig._
import pureconfig.generic.ProductHint
import pureconfig.generic.auto._
case class WorkerConfig(slaveId: String, grpc: GrpcConfig, mongodb: MongoDbConfig, redis: RedisConfig, kafka: KafkaConfiguration)

object WorkerConfig {

  implicit val confHint: ProductHint[WorkerConfig] = ProductHint[WorkerConfig](ConfigFieldMapping(CamelCase, KebabCase))

  def load(): Either[Exception, WorkerConfig] = ConfigSource.default.load[WorkerConfig] match {
    case Left(failures) => Left(new Exception(failures.toString))
    case Right(value) => Right(value)
  }

  case class GrpcConfig(client: GrpcEndpoint, server: GrpcEndpoint)

  case class GrpcEndpoint(
    host: String,
    port: Int,
    endPoint: String)

  case class MongoDbConfig(
    host: String,
    port: Int,
    url: String,
    database: String,
    itemsColName: String,
    buyActionsColName: String,
    pawnActionsColName: String,
    sellActionColName: String)

  case class RedisConfig(
    host: String,
    port: Int,
    url: String,
    interactionsKeyPrefix: String,
    branchesKeyPrefix: String,
    fraudstersKey: String)

  case class KafkaConfiguration( groupId: String,
                                 itemsTopic: String,
                                 buyEventsTopic: String,
                                 sellEventsTopic: String,
                                 pawnEventsTopic: String)


}

