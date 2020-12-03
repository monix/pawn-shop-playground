package monix.mini.platform.slave.config

import monix.mini.platform.slave.config.SlaveConfig.GrpcServerConfiguration
//import io.circe._
//import io.circe.generic.auto._
//import io.circe.generic.semiauto._
import pureconfig._
import pureconfig.generic.ProductHint
import pureconfig.generic.auto._

case class SlaveConfig(slaveId: String, grpcServer: GrpcServerConfiguration, masterServer: GrpcServerConfiguration)

object SlaveConfig {

  implicit val confHint: ProductHint[SlaveConfig] = ProductHint[SlaveConfig](ConfigFieldMapping(CamelCase, KebabCase))

  def load(): SlaveConfig = loadConfigOrThrow[SlaveConfig]

  case class GrpcServerConfiguration(
    host: String,
    port: Int,
    endPoint: String)

}

