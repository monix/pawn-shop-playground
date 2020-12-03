package monix.mini.platform

import monix.mini.platform.protocol.{OperationEvent, OperationType, TransactionEvent}

package object protocol {

  implicit class ProtoToScalaConverter(transactionEvent: TransactionEvent) {
    def toEntity: TransactionEventEntity = {
      val TransactionEvent(sender, receiver, amount, _) = transactionEvent
      TransactionEventEntity("", sender, receiver, amount)
    }
  }

  implicit class OperationEventConverter(operationEvent: OperationEvent) {
    def toEntity: OperationEventEntity = {
      val OperationEvent(client, amount, location, operationType, _) = operationEvent
      OperationEventEntity(client, amount, location, String.valueOf(operationType))
    }
  }


  implicit class FetchReplyConverter(fetchReply: FetchReply) {
    def toEntity: FetchReplyView = {
      val FetchReply(transactions, operations, _) = fetchReply
      FetchReplyView(transactions.map(_.toEntity), operations.map(_.toEntity))
    }
  }

  case class FetchReplyView(transactions: Seq[TransactionEventEntity], operations: Seq[OperationEventEntity])

  case class TransactionEventEntity(id: String, sender: String, receiver: String, amount: Long) {
    def toProto: TransactionEvent = {
      TransactionEvent.of(sender, receiver, amount)
    }
  }

  case class OperationEventEntity(client: String, amount: Long, location: String, operationType: String) {
    def toProto: OperationEvent = OperationEvent.of(client, amount, location, OperationType.fromName(operationType).getOrElse(OperationType.WITHDRAW))
  }


}
