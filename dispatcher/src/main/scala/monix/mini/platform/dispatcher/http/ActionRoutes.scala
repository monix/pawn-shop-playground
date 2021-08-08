package monix.mini.platform.dispatcher.http

import com.typesafe.scalalogging.LazyLogging
import io.circe.generic.auto._
import monix.eval.Task
import monix.mini.platform.dispatcher.Dispatcher
import monix.mini.platform.dispatcher.kafka.KafkaPublisher
import monix.mini.platform.dispatcher.model.{ BuyEntity, PawnEntity, SellEntity }
import monix.mini.platform.protocol.{ Buy, Pawn, Sell }
import org.http4s.circe.jsonOf
import org.http4s.dsl.Http4sDsl
import org.http4s.{ EntityDecoder, HttpRoutes, Response }

import scala.language.implicitConversions

trait ActionRoutes extends Http4sDsl[Task] with LazyLogging {

  implicit val buyEncoder: EntityDecoder[Task, BuyEntity] = jsonOf[Task, BuyEntity]
  implicit val sellEncoder: EntityDecoder[Task, SellEntity] = jsonOf[Task, SellEntity]
  implicit val pawnEncoder: EntityDecoder[Task, PawnEntity] = jsonOf[Task, PawnEntity]

  implicit val buyPublisher: KafkaPublisher[Buy]
  implicit val sellPublisher: KafkaPublisher[Sell]
  implicit val pawnPublisher: KafkaPublisher[Pawn]

  val dispatcher: Dispatcher

  lazy val actionRoutes: HttpRoutes[Task] = HttpRoutes.of[Task] {

    case req @ POST -> Root / "item" / "action" / "buy" =>
      val buyEvent: Task[Buy] = req.as[BuyEntity].map(_.toProto)
      logger.info(s"Received Buy item event.")
      buyEvent.flatMap(dispatcher.publish(_))

    case req @ POST -> Root / "item" / "action" / "sell" =>
      val sellEvent: Task[Sell] = req.as[SellEntity].map(_.toProto)
      logger.info(s"Received Sell item event.")
      sellEvent.flatMap(dispatcher.publish(_))

    case req @ POST -> Root / "item" / "action" / "pawn" =>
      val pawnEvent: Task[Pawn] = req.as[PawnEntity].map(_.toProto)
      logger.info(s"Received Pawn item event.")
      pawnEvent.flatMap(dispatcher.publish(_))
  }

  implicit def plainHttpResponse(task: Task[Unit]): Task[Response[Task]] = {
    task.redeem(
      ex => {
        logger.error(s"Failed to process action event, returning $InternalServerError.", ex)
        Response(status = InternalServerError)
      },
      _ => Response(status = Ok))
  }
}

