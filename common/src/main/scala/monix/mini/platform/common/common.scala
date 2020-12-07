package monix.mini.platform

import scala.concurrent.Future

package object protocol {

  implicit class ProtoToScalaConverter(transactionEvent: TransactionEvent) {
    def toEntity: TransactionEventEntity = {
      val TransactionEvent(sender, receiver, amount, _) = transactionEvent
      TransactionEventEntity("", sender, receiver, amount)
    }
  }

  implicit class OperationEventConverter(operationEvent: OperationEvent) {
    def toEntity: OperationEventEntity = {
      val OperationEvent(client, amount, branch, operationType, _) = operationEvent
      OperationEventEntity(client, amount, branch, String.valueOf(operationType))
    }
  }

  case class TransactionEventEntity(id: String, sender: String, receiver: String, amount: Long) {
    def toProto: TransactionEvent = {
      TransactionEvent.of(sender, receiver, amount)
    }
  }

  case class OperationEventEntity(client: String, amount: Long, branch: String, operationType: String) {
    def toProto: OperationEvent = OperationEvent.of(client, amount, branch, OperationType.fromName(operationType).getOrElse(OperationType.WITHDRAW))
  }

  //fetch
  sealed trait FetchReplyView
  case class FetchAllReplyView(transactions: Seq[TransactionEventEntity], operations: Seq[OperationEventEntity],
    interactions: Seq[String], branches: Seq[String]) extends FetchReplyView
  case class FetchTransactionsReplyView(transactions: Seq[TransactionEventEntity]) extends FetchReplyView
  case class FetchOperationsReplyView(operations: Seq[OperationEventEntity]) extends FetchReplyView
  case class FetchInteractionsReplyView(interactions: Seq[String]) extends FetchReplyView
  case class FetchBranchesReplyView(branches: Seq[String]) extends FetchReplyView

  implicit class FetchAllReplyConverter(fetchReply: FetchAllReply) {
    def toEntity: FetchAllReplyView = {
      val FetchAllReply(transactions, operations, interactions, branches, _) = fetchReply
      FetchAllReplyView(transactions.map(_.toEntity), operations.map(_.toEntity), interactions, branches)
    }
  }

  implicit class FetchTransactionsReplyConverter(fetchReply: FetchTransactionsReply) {
    def toEntity = FetchTransactionsReplyView(fetchReply.transactions.map(_.toEntity))
  }
  implicit class FetchOperationsReplyConverter(fetchReply: FetchOperationsReply) {
    def toEntity = FetchOperationsReplyView(fetchReply.operations.map(_.toEntity))
  }
  implicit class FetchInteractionsReplyConverter(fetchReply: FetchInteractionsReply) {
    def toEntity = FetchInteractionsReplyView(fetchReply.interactions)
  }
  implicit class FetchBranchesReplyConverter(fetchReply: FetchBranchesReply) extends {
    def toEntity = FetchBranchesReplyView(fetchReply.branches)
  }

}
