package monix.mini.platform.dispatcher.http

import cats.effect.{ConcurrentEffect, ContextShift}
import com.typesafe.scalalogging.LazyLogging
import monix.eval.Task
import monix.execution.Scheduler
import org.http4s.implicits._
import monix.mini.platform.dispatcher.Dispatcher
import monix.mini.platform.dispatcher.config.DispatcherConfig
import monix.mini.platform.dispatcher.kafka.KafkaPublisher
import monix.mini.platform.protocol.{Buy, Item, Pawn, Sell}
import org.http4s.server.Router
import org.http4s.server.blaze.BlazeServerBuilder

class HttpServer(config: DispatcherConfig, val dispatcher: Dispatcher, s: Scheduler)(implicit ce: ConcurrentEffect[Task]) extends CoreRoutes with ActionRoutes with LazyLogging {

    logger.info(s"Starting http server on endpoint: ${config.httpServer.endPoint}")
    val kafkaConfig = config.kafka
    implicit val kafkaProducerConfig = kafkaConfig.producerConfig
    override val itemPublisher: KafkaPublisher[Item] = new KafkaPublisher[Item](kafkaConfig.itemsTopic)
    override val buyPublisher: KafkaPublisher[Buy] = new KafkaPublisher[Buy](kafkaConfig.buyEventsTopic)
    override val sellPublisher: KafkaPublisher[Sell] = new KafkaPublisher[Sell](kafkaConfig.sellEventsTopic)
    override val pawnPublisher: KafkaPublisher[Pawn] = new KafkaPublisher[Pawn](kafkaConfig.pawnEventsTopic)

    val httpApp = Router("/item" -> coreRoutes, "/action" -> actionRoutes).orNotFound

    def bindAndNeverTerminate: Task[Unit] =
        BlazeServerBuilder[Task](s)
            .bindHttp(config.httpServer.port, config.httpServer.host)
            .withHttpApp(httpApp)
            .resource.use(_ => Task.never)
}
