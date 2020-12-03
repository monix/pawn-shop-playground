package monix.mini.platform.master.http

import org.http4s.circe.{jsonEncoderOf, jsonOf}
import com.typesafe.scalalogging.LazyLogging
import io.circe.generic.auto._
import org.http4s.{EntityDecoder, HttpRoutes, MediaRange, Response}
import monix.eval.Task
import monix.mini.platform.master.Dispatcher
import monix.mini.platform.protocol.{EventResult, FetchReplyView, FetchRequest, OperationEvent, OperationEventEntity, OperationType, ResultStatus, TransactionEvent, TransactionEventEntity}
import org.http4s.dsl.Http4sDsl

trait UserRoutes extends Http4sDsl[Task] with LazyLogging {

  implicit val transactionEventDecoder = jsonOf[Task, TransactionEventEntity]
  implicit val operationEventDecoder = jsonOf[Task, OperationEventEntity]
  implicit val fetchReplyEncoder = jsonEncoderOf[Task, FetchReplyView]


  val dispatcher: Dispatcher

  lazy val routes: HttpRoutes[Task] = HttpRoutes.of[Task] {

    case _ @ GET -> Root => {
      logger.info("Index request received")
      Ok("Monix Mini Platform")
    }

    case _ @ GET -> Root / "find" / client => {
      logger.info(s"Read one received request.")
      dispatcher.dispatch(FetchRequest.of(client))
        .map(response => Response(status = Ok).withEntity(response.toEntity)(fetchReplyEncoder))
    }

    case req @ POST -> Root / "transaction" => {
      val insertRequest: Task[TransactionEvent] = req.as[TransactionEventEntity].map(_.toProto)
      logger.info(s"Transaction received one received")
      insertRequest.flatMap(dispatcher.dispatch(_)).map {
        case EventResult(ResultStatus.INSERTED, _) =>  Response(status = Ok).withEntity(ResultStatus.INSERTED.toString())
        case EventResult(ResultStatus.FAILED, _)  =>  Response(status = NotFound).withEntity(ResultStatus.FAILED.toString())
      }
    }

    case req @ POST -> Root / "operation" => {
      val insertRequest: Task[OperationEvent] = req.as[OperationEventEntity].map(_.toProto)
      logger.info(s"Insert one received")

      insertRequest.flatMap(dispatcher.dispatch(_)).map {

        case EventResult(ResultStatus.INSERTED, _) =>  Response(status = Ok).withEntity(ResultStatus.INSERTED.toString())
        case EventResult(ResultStatus.FAILED, _)  =>  Response(status = NotFound).withEntity(ResultStatus.FAILED.toString())
      }
    }

  }

}
