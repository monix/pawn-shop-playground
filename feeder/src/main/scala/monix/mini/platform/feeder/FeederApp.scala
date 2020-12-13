package monix.mini.platform.feeder

import cats.effect.{ExitCode, IOApp, IO => CatsIO}
import com.typesafe.scalalogging.LazyLogging
import io.lettuce.core.RedisClient
import io.lettuce.core.api.StatefulRedisConnection
import monix.connect.redis.RedisSet
import monix.eval.Task

import scala.concurrent.duration._
import monix.connect.s3.S3
import monix.bio.{IO, UIO}
import pureconfig.error.ConfigReaderFailures
import monix.execution.Scheduler.Implicits.global
import monix.mini.platform.feeder.config.FeederConfig
import monix.mini.platform.feeder.config.FeederConfig.S3Config

object FeederApp extends IOApp with LazyLogging {

  def run(args: List[String]): CatsIO[ExitCode] = {

    def downloadAndFeed(implicit
      s3: S3,
      redisConnection: StatefulRedisConnection[String, String],
      feederConf: FeederConfig): Task[Long] = {
      logger.info("Downloading and feeding fraudster data.")
      val S3Config(bucket, key) = feederConf.s3
      s3.download(bucket, key)
        .map(new String(_).split("\n"))
        .flatMap(arr => RedisSet.sadd(feederConf.redis.fraudstersKey, arr: _*))
    }

    val scheduleFeeder: FeederConfig => IO[Throwable, ExitCode] = { implicit feederConf =>
      implicit val redisConnection = RedisClient.create(feederConf.redis.url).connect()
      IO.from {
        S3.fromConfig.use { implicit s3 =>
          Task(global.scheduleWithFixedDelay(1.second, 1.second)(downloadAndFeed.runToFuture))
            .flatMap(_ => Task.never)
        }
      }.as(ExitCode.Success)
    }

    val handleConfigFailures: ConfigReaderFailures => UIO[ExitCode] = { configFailures =>
      logger.error(s"Failed to load application config, cause: ${configFailures.prettyPrint()}")
      UIO.now(ExitCode.Error)
    }

    FeederConfig.load.redeemWith(handleConfigFailures(_), scheduleFeeder(_)).to[CatsIO]
  }
}
