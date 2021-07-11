package monix.mini.platform.feeder.config

import io.circe.{ Encoder, Json }
import monix.bio.IO
import monix.mini.platform.feeder.config.FeederConfig.{ RedisConfig, S3Config }
import pureconfig.error.ConfigReaderFailures
import pureconfig.generic.ProductHint
import pureconfig.{ CamelCase, ConfigFieldMapping, ConfigSource }
import pureconfig.generic.auto._

import scala.concurrent.duration.FiniteDuration

case class FeederConfig(feedInterval: FiniteDuration, s3: S3Config, redis: RedisConfig)

object FeederConfig {

  implicit val encodeDuration: Encoder[FiniteDuration] = Encoder.instance(duration ⇒ Json.fromString(duration.toString))
  implicit val confHint: ProductHint[FeederConfig] = ProductHint[FeederConfig](ConfigFieldMapping(CamelCase, CamelCase))
  implicit val redisConfHint: ProductHint[RedisConfig] = ProductHint[RedisConfig](ConfigFieldMapping(CamelCase, CamelCase))

  case class RedisConfig(
    host: String,
    port: Int,
    url: String,
    fraudstersKey: String)

  case class S3Config(
    bucket: String,
    key: String)

  def load(): IO[ConfigReaderFailures, FeederConfig] = IO.fromEither(ConfigSource.default.load[FeederConfig])

}
