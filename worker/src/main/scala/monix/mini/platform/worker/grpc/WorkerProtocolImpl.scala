package monix.mini.platform.worker.grpc

import com.mongodb.client.model.Filters
import com.typesafe.scalalogging.LazyLogging
import monix.mini.platform.protocol.{ Buy, FetchByCategoryRequest, FetchByIdRequest, FetchByNameRequest, FetchByStateRequest, FetchItemResponse, FetchItemsResponse, Item, ItemWithHistory, Pawn, Sell }
import monix.mini.platform.protocol.WorkerProtocolGrpc.WorkerProtocol

import scala.concurrent.Future
import monix.connect.mongodb.client.CollectionOperator
import monix.eval.Task
import monix.execution.Scheduler

private class WorkerProtocolImpl(itemCol: CollectionOperator[Item], buyCol: CollectionOperator[Buy], sellCol: CollectionOperator[Sell], pawnCol: CollectionOperator[Pawn])(implicit scheduler: Scheduler) extends WorkerProtocol with LazyLogging {

  override def fetchItemById(request: FetchByIdRequest): Future[FetchItemResponse] = {
    logger.debug(s"Received fetch by id request: $request")
    itemCol.source.find(Filters.eq("id", request.id))
      .headOptionL
      .flatMap(_.map(fetchItemHistory(_).map(Some(_))).getOrElse(Task.pure(None)))
      .map(FetchItemResponse.of)
      .onErrorHandleWith { ex =>
        logger.error("Failed to find item by id.", ex)
        Task.raiseError(ex)
      }.runToFuture
  }

  override def fetchItemsByName(request: FetchByNameRequest): Future[FetchItemsResponse] = {
    logger.debug(s"Received fetch by name `${request.name}` request.")
    itemCol.source.find(Filters.eq("name", request.name))
      .take(request.limit)
      .mapEvalF(fetchItemHistory)
      .toListL
      .map(FetchItemsResponse.of)
      .onErrorHandleWith { ex =>
        logger.error("Failed to find item by name.", ex)
        Task.raiseError(ex)
      }.runToFuture
  }

  override def fetchItemsByCategory(request: FetchByCategoryRequest): Future[FetchItemsResponse] = {
    logger.debug(s"Received fetch by category `${request.category}` request.")
    itemCol.source.find(Filters.eq("category", request.category.name))
      .take(request.limit)
      .mapEvalF(fetchItemHistory)
      .toListL.map(FetchItemsResponse.of).onErrorHandleWith { ex =>
        logger.error("Failed to find item by category.", ex)
        Task.raiseError(ex)
      }.runToFuture
  }

  def findByItemId[A](col: CollectionOperator[A], item: Item): Task[List[A]] = col.source.find(Filters.eq("itemId", item.id)).toListL

  def fetchItemHistory[A](item: Item): Task[ItemWithHistory] = {
    val buyActions = findByItemId(buyCol, item)
    val sellActions = findByItemId(sellCol, item)
    val pawnActions = findByItemId(pawnCol, item)
    Task.parZip3(buyActions, sellActions, pawnActions).map(t3 => ItemWithHistory.of(Some(item), t3._1, t3._2, t3._3))
  }

  override def fetchItemsByState(request: FetchByStateRequest): Future[FetchItemsResponse] = {
    logger.debug(s"Received fetch by `${request.state}` state request.")
    itemCol.source.find(Filters.eq("state", request.state.toString()))
      .take(request.limit)
      .mapEval(fetchItemHistory)
      .toListL.map(FetchItemsResponse.of)
      .onErrorHandleWith { ex =>
        logger.error("Failed to find item by state.", ex)
        Task.raiseError(ex)
      }.runToFuture
  }

}
