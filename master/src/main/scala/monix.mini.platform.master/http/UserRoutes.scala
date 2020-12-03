package monix.mini.platform.master.http

import org.http4s.circe.jsonOf
import com.typesafe.scalalogging.LazyLogging
import io.circe.generic.auto._
import org.http4s.{HttpRoutes, Response}
import monix.eval.Task
import monix.mini.platform.master.http.UserRoutes.{OperationEventView, TransactionEventView}
import monix.mini.platform.master.Dispatcher
import monix.mini.platform.protocol.{EventResult, FetchRequest, OperationEvent, ResultStatus, TransactionEvent, OperationType}
import org.http4s.dsl.Http4sDsl

trait UserRoutes extends Http4sDsl[Task] with LazyLogging {

  implicit val transactionEventDecoder = jsonOf[Task, TransactionEventView]
  implicit val operationEventDecoder = jsonOf[Task, OperationEventView]

  val dispatcher: Dispatcher

  lazy val routes: HttpRoutes[Task] = HttpRoutes.of[Task] {

    case _ @ GET -> Root => {
      logger.info("Index request received")
      Ok("Monix Mini Platform")
    }

    case _ @ GET -> Root / "find" / client => {
      logger.info(s"Read one received request.")
      dispatcher.dispatch(FetchRequest.of(client))
        .map(response => Response(status = Ok).withEntity(response.toByteArray))
    }

    case req @ POST -> Root / "transaction" => {
      val insertRequest: Task[TransactionEvent] = req.as[TransactionEventView].map(_.toProto)
      logger.info(s"Transaction received one received")
      insertRequest.flatMap(dispatcher.dispatch(_)).map {
        case EventResult(ResultStatus.INSERTED, _) =>  Response(status = Ok).withEntity(ResultStatus.INSERTED.toString())
        case EventResult(ResultStatus.FAILED, _)  =>  Response(status = NotFound).withEntity(ResultStatus.FAILED.toString())
      }
    }

    case req @ POST -> Root / "operation" => {
      val insertRequest: Task[OperationEvent] = req.as[OperationEventView].map(_.toProto)
      logger.info(s"Insert one received")

      insertRequest.flatMap(dispatcher.dispatch(_)).map {

        case EventResult(ResultStatus.INSERTED, _) =>  Response(status = Ok).withEntity(ResultStatus.INSERTED.toString())
        case EventResult(ResultStatus.FAILED, _)  =>  Response(status = NotFound).withEntity(ResultStatus.FAILED.toString())
      }
    }

  }

}

object UserRoutes {
  case class TransactionEventView(id: String, sender: String, receiver: String, amount: Long) {
    def toProto: TransactionEvent = {
      TransactionEvent.of(sender, receiver, amount)
    }
  }
  case class OperationEventView(id: String, client: String, amount: Long, location: String, operationType: OperationType) {
    def toProto: OperationEvent = {
      OperationEvent.of(client, amount, location, operationType)
    }
  }

}
