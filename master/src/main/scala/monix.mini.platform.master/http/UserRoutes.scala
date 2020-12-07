package monix.mini.platform.master.http

import org.http4s.circe.{jsonEncoderOf, jsonOf}
import com.typesafe.scalalogging.LazyLogging
import io.circe.generic.auto._
import org.http4s.{HttpRoutes, Response}
import monix.eval.Task
import monix.mini.platform.master.algebra.Fetch.{allFetch, branchesFetch, interactionsFetch, operationsFetch, transactionsFetch}
import monix.mini.platform.master.Dispatcher
import monix.mini.platform.protocol.{EventResult, FetchAllReplyView, FetchBranchesReplyView, FetchInteractionsReplyView, FetchOperationsReplyView, FetchRequest, FetchTransactionsReplyView, OperationEvent, OperationEventEntity, ResultStatus, TransactionEvent, TransactionEventEntity}
import org.http4s.dsl.Http4sDsl

trait UserRoutes extends Http4sDsl[Task] with LazyLogging {

  implicit val transactionEventDecoder = jsonOf[Task, TransactionEventEntity]
  implicit val operationEventDecoder = jsonOf[Task, OperationEventEntity]
  implicit val fetchReplyEncoder = jsonEncoderOf[Task, FetchAllReplyView]
  implicit val fetchTransactionsReplyEncoder = jsonEncoderOf[Task, FetchTransactionsReplyView]
  implicit val fetchOperationsReplyEncoder = jsonEncoderOf[Task, FetchOperationsReplyView]
  implicit val fetchInteractionsReplyEncoder = jsonEncoderOf[Task, FetchInteractionsReplyView]
  implicit val fetchBranchesReplyEncoder = jsonEncoderOf[Task, FetchBranchesReplyView]

  val dispatcher: Dispatcher

  lazy val routes: HttpRoutes[Task] = HttpRoutes.of[Task] {

    case _ @ GET -> Root => Ok("Monix Mini Platform")

    case _ @ GET -> Root / "fetch" / "all" / client => {
      logger.info(s"Fetch all request received.")
      dispatcher.dispatch(FetchRequest.of(client))(allFetch)
        .map(Response(status = Ok).withEntity(_))
    }

    case _ @ GET -> Root / "fetch" / "transactions" / client => {
      logger.info(s"Fetch transactions request received.")
      dispatcher.dispatch(FetchRequest.of(client))(transactionsFetch)
        .map(Response(status = Ok).withEntity(_))
    }

    case _ @ GET -> Root / "fetch" / "operations" / client => {
      logger.info(s"Fetch operations request received.")
      dispatcher.dispatch(FetchRequest.of(client))(operationsFetch)
        .map(Response(status = Ok).withEntity(_))
    }

    case _ @ GET -> Root / "fetch" / "interactions" / client => {
      logger.info(s"Fetch interactions request received.")
      dispatcher.dispatch(FetchRequest.of(client))(interactionsFetch)
        .map(Response(status = Ok).withEntity(_))
    }

    case _ @ GET -> Root / "fetch" / "branches" / client => {
      logger.debug(s"Fetch branches request received.")
      dispatcher.dispatch(FetchRequest.of(client))(branchesFetch)
        .map(Response(status = Ok).withEntity(_))
    }

    case req @ POST -> Root / "transaction" => {
      val insertRequest: Task[TransactionEvent] = req.as[TransactionEventEntity].map(_.toProto)
      logger.info(s"Transaction received one received")
      insertRequest.flatMap(dispatcher.dispatch(_)).map {
        case EventResult(ResultStatus.FRAUDULENT, _) =>  Response(status = Unauthorized).withEntity(ResultStatus.FRAUDULENT.toString())
        case EventResult(ResultStatus.FAILED, _)  =>  Response(status = NotFound).withEntity(ResultStatus.FAILED.toString())
        case EventResult(status, _) =>  Response(status = Ok).withEntity(status.toString())

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
