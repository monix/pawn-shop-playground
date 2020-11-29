package monix.mini.platform.master.config

import SlaveConfig.GrpcServerConfiguration
import io.circe._
import io.circe.generic.auto._
import io.circe.generic.semiauto._
import pureconfig._
import pureconfig.generic.ProductHint
import pureconfig.generic.auto._


case class SlaveConfig(grpcServer: GrpcServerConfiguration)

object SlaveConfig {

  implicit val confHint: ProductHint[SlaveConfig] = ProductHint[SlaveConfig](ConfigFieldMapping(CamelCase, KebabCase))
  implicit val encodeAppConfig: Encoder[SlaveConfig] = deriveEncoder

  def load(): SlaveConfig = loadConfigOrThrow[SlaveConfig]

  case class GrpcServerConfiguration(
    host: String,
    port: Int,
    endPoint: String)

}

