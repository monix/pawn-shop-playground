package monix.mini.platform.master.http

import cats.effect.ConcurrentEffect
import com.typesafe.scalalogging.LazyLogging
import monix.eval.Task
import monix.execution.Scheduler
import monix.mini.platform.config.DispatcherConfig
import org.http4s.server.blaze._
import org.http4s.implicits._
import monix.mini.platform.master.Dispatcher
import monix.mini.platform.master.kafka.KafkaPublisher
import monix.mini.platform.protocol.{ Buy, Item, Pawn, Sell }
import org.http4s.server.blaze.BlazeServerBuilder
import scala.concurrent.ExecutionContext

class HttpServer(config: DispatcherConfig, s: Scheduler)(implicit concurrentEffect: ConcurrentEffect[Task]) extends CoreRoutes with ActionRoutes with LazyLogging {

    logger.info(s"Starting http server on endpoint: ${config.httpServer.endPoint}")

    override val dispatcher: Dispatcher = new Dispatcher(config)
    override val itemPublisher: KafkaPublisher[Item] = new KafkaPublisher[Item](config.kafkaConfig.itemsTopicConfig)
    override val buyPublisher: KafkaPublisher[Buy] = new KafkaPublisher[Buy](config.kafkaConfig.buyEventsTopicConfig)
    override val sellPublisher: KafkaPublisher[Sell] = new KafkaPublisher[Sell](config.kafkaConfig.sellTopicConfig)
    override val pawnPublisher: KafkaPublisher[Pawn] = new KafkaPublisher[Pawn](config.kafkaConfig.pawnTopicConfig)

    def bindAndNeverTerminate: Task[Unit] =
        BlazeServerBuilder[Task](s)
            .bindHttp(config.httpServer.port, config.httpServer.host)
            .withHttpApp(routes.orNotFound)
            .resource.use(_ => Task.never)
}
