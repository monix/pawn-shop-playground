package monix.mini.platform.master.http

import com.typesafe.scalalogging.LazyLogging
import io.circe.generic.auto._
import monix.eval.Task
import monix.mini.platform.master.typeclass.Fetch.branchesFetch
import monix.mini.platform.master.{ Dispatcher, KafkaPublisher }
import monix.mini.platform.protocol.{ Buy, Pawn, Sell }
import org.http4s.circe.jsonOf
import org.http4s.dsl.Http4sDsl
import org.http4s.{ HttpRoutes, Response }

trait ActionRoutes extends Http4sDsl[Task] with LazyLogging {


  case class BuyEntity(clientId: String, itemId: String, price: Long, date: String, profit: String) {
    def toProto: Buy = Buy(clientId = clientId, itemId = itemId, price = price, date = date)
  }

  case class SellEntity(clientId: String, itemId: String, price: Long, date: String, profit: String) {
    def toProto: Sell = Sell(clientId = clientId, itemId = itemId, price = price, date = date, profit = profit)
  }

  case class PawnEntity(clientId: String, itemId: String, price: Long, tax: Int, limitDays: Int, profit: String) {
    def toProto: Pawn = Pawn(clientId = clientId, itemId = itemId, price = price, tax = tax)
  }

  implicit val buyEncoder = jsonOf[Task, BuyEntity]
  implicit val sellEncoder = jsonOf[Task, SellEntity]
  implicit val pawnEncoder = jsonOf[Task, PawnEntity]

  implicit val buyEventPublisher: KafkaPublisher[Buy]
  implicit val sellEventPublisher: KafkaPublisher[Sell]
  implicit val pawnEventPublisher: KafkaPublisher[Pawn]

  val dispatcher: Dispatcher

  lazy val routes: HttpRoutes[Task] = HttpRoutes.of[Task] {


    case req@POST -> Root / "item" / "action" / "buy" =>
      val buyEvent: Task[Buy] = req.as[BuyEntity].map(_.toProto)
      logger.info(s"Received Buy Item event.")
      buyEvent.flatMap(dispatcher.publish(_, retries = 3)).toHttpResponse

    case req@POST -> Root / "item" / "action" / "sell" =>
      val sellEvent: Task[Sell] = req.as[SellEntity].map(_.toProto)
      logger.info(s"Received Sell item event.")
      sellEvent.flatMap(dispatcher.publish(_, retries = 3)).toHttpResponse


    case req@POST -> Root / "item" / "action" / "pawn" =>
      val pawnEvent: Task[Pawn] = req.as[PawnEntity].map(_.toProto)
      logger.info(s"Received Pawn item event.")
      pawnEvent.flatMap(dispatcher.publish(_, retries = 3)).toHttpResponse

  }

  implicit class ExtendedUnitTask[A](task: Task[Unit]) {
    def toHttpResponse: Task[Response[Task]] = {
      task.redeem(
        ex => {
          logger.error(s"Failed to process event, returning status code ${InternalServerError.code}.", ex)
          Response(status = InternalServerError)
        }
        ,
        _ => Response(status = Ok)
      )
    }
  }
}


