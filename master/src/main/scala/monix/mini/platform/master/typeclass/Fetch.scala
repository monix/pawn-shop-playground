package monix.mini.platform.master.typeclass

import monix.mini.platform.master.SlaveRef
import monix.mini.platform.protocol.{ FetchAllReply, FetchAllReplyView, FetchBranchesReply, FetchBranchesReplyView, FetchInteractionsReply, FetchInteractionsReplyView, FetchOperationsReply, FetchOperationsReplyView, FetchRequest, FetchTransactionsReply, FetchTransactionsReplyView }

import scala.concurrent.Future

trait Fetch[Proto, View] {
  val send: (SlaveRef, FetchRequest) => Future[Proto]
  def toView(fetchReply: Proto): View
  val defaultInstance: Proto
}

object Fetch {

  implicit val allFetch = new Fetch[FetchAllReply, FetchAllReplyView] {
    override val defaultInstance: FetchAllReply = FetchAllReply.defaultInstance
    override val send = (slaveRef: SlaveRef, fetchRequest: FetchRequest) => slaveRef.stub.fetchAll(fetchRequest)
    override def toView(fetchReply: FetchAllReply): FetchAllReplyView = {
      val FetchAllReply(transactions, operations, interactions, branches, _) = fetchReply
      FetchAllReplyView(transactions.map(_.toEntity), operations.map(_.toEntity), interactions, branches)
    }
  }

  implicit val transactionsFetch = new Fetch[FetchTransactionsReply, FetchTransactionsReplyView] {
    override val defaultInstance: FetchTransactionsReply = FetchTransactionsReply.defaultInstance
    override val send = (slaveRef: SlaveRef, fetchRequest: FetchRequest) =>
      slaveRef.stub.fetchTransactions(fetchRequest)
    override def toView(fetchReply: FetchTransactionsReply): FetchTransactionsReplyView = {
      FetchTransactionsReplyView(fetchReply.transactions.map(_.toEntity))
    }
  }

  implicit val operationsFetch = new Fetch[FetchOperationsReply, FetchOperationsReplyView] {
    override val defaultInstance: FetchOperationsReply = FetchOperationsReply.defaultInstance
    override val send = (slaveRef: SlaveRef, fetchRequest: FetchRequest) =>
      slaveRef.stub.fetchOperations(fetchRequest)
    override def toView(fetchReply: FetchOperationsReply): FetchOperationsReplyView = {
      FetchOperationsReplyView(fetchReply.operations.map(_.toEntity))
    }
  }

  implicit val interactionsFetch = new Fetch[FetchInteractionsReply, FetchInteractionsReplyView] {
    override val defaultInstance: FetchInteractionsReply = FetchInteractionsReply.defaultInstance
    override val send = (slaveRef: SlaveRef, fetchRequest: FetchRequest) =>
      slaveRef.stub.fetchInteractions(fetchRequest)
    override def toView(fetchReply: FetchInteractionsReply): FetchInteractionsReplyView = {
      FetchInteractionsReplyView(fetchReply.interactions)
    }
  }

  implicit val branchesFetch = new Fetch[FetchBranchesReply, FetchBranchesReplyView] {
    override val defaultInstance: FetchBranchesReply = FetchBranchesReply.defaultInstance
    override val send = (slaveRef: SlaveRef, fetchRequest: FetchRequest) =>
      slaveRef.stub.fetchBranches(fetchRequest)
    override def toView(fetchReply: FetchBranchesReply): FetchBranchesReplyView = {
      FetchBranchesReplyView(fetchReply.branches)
    }
  }
}