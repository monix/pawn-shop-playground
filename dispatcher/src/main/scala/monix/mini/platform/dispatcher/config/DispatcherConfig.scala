package monix.mini.platform.dispatcher.config

import java.time.LocalDate
import java.time.format.DateTimeFormatter.ISO_DATE
import DispatcherConfig.{ GrpcServerConfiguration, HttpServerConfiguration, KafkaConfiguration }
import io.circe._
import io.circe.generic.auto._
import io.circe.generic.semiauto._
import io.circe.syntax._
import monix.kafka.KafkaProducerConfig
import pureconfig._
import pureconfig.configurable.localDateConfigConvert
import pureconfig.generic.ProductHint
import pureconfig.generic.auto._
import scala.concurrent.duration.FiniteDuration

case class DispatcherConfig(httpServer: HttpServerConfiguration, grpcTimeout: FiniteDuration, grpcServer: GrpcServerConfiguration, kafka: KafkaConfiguration) {
  def toJson: String = this.asJson.noSpaces
}

object DispatcherConfig {

  implicit val confHint: ProductHint[DispatcherConfig] = ProductHint[DispatcherConfig](ConfigFieldMapping(CamelCase, KebabCase))

  implicit val localDateConvert: ConfigConvert[LocalDate] = localDateConfigConvert(ISO_DATE)

  implicit val encodeAppConfig: Encoder[DispatcherConfig] = deriveEncoder
  implicit val encodeDuration: Encoder[FiniteDuration] = Encoder.instance(duration ⇒ Json.fromString(duration.toString))
  implicit val encodeLocalDate: Encoder[LocalDate] = Encoder.instance(date ⇒ Json.fromString(date.format(ISO_DATE)))

  def load(): DispatcherConfig = ConfigSource.default.loadOrThrow[DispatcherConfig]

  case class HttpServerConfiguration(
    host: String,
    port: Int,
    endPoint: String)

  case class GrpcServerConfiguration(
    host: String,
    port: Int,
    endPoint: String)

  case class KafkaConfiguration(
    itemsTopic: String,
    buyEventsTopic: String,
    sellEventsTopic: String,
    pawnEventsTopic: String) {

    val producerConfig: KafkaProducerConfig = KafkaProducerConfig.default

  }

  case class KafkaTopicConfig(topicName: String, producerConfig: KafkaProducerConfig)

}

