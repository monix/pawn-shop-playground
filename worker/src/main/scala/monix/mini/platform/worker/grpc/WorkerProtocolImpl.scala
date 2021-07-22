package monix.mini.platform.worker.grpc

import com.mongodb.client.model.Filters
import com.typesafe.scalalogging.LazyLogging
import monix.mini.platform.protocol.{ FetchByCategoryRequest, FetchByIdRequest, FetchByNameRequest, FetchByStateRequest, FetchItemResponse, FetchItemsResponse, Item }
import monix.mini.platform.protocol.WorkerProtocolGrpc.WorkerProtocol
import scala.concurrent.Future
import monix.connect.mongodb.client.CollectionOperator
import monix.execution.Scheduler

private class WorkerProtocolImpl(itemCol: CollectionOperator[Item])(implicit scheduler: Scheduler) extends WorkerProtocol with LazyLogging {

  override def fetchItemById(request: FetchByIdRequest): Future[FetchItemResponse] = {
    logger.debug(s"Received fetch by id request: ${request}")
    itemCol.source.find(Filters.eq("id", request.id)).headOptionL.map(FetchItemResponse.of).runToFuture
  }

  override def fetchItemsByName(request: FetchByNameRequest): Future[FetchItemsResponse] = {
    logger.debug(s"Received fetch by `${request.name}` name request.")
    itemCol.source.find(Filters.eq("name", request.name))
      .take(request.limit).toListL.map(FetchItemsResponse.of).runToFuture
  }

  override def fetchItemsByCategory(request: FetchByCategoryRequest): Future[FetchItemsResponse] = {
    logger.debug(s"Received fetch by `${request.category}` category request.")
    itemCol.source.find(Filters.eq("category", request.category.name))
      .take(request.limit).toListL.map(FetchItemsResponse.of).runToFuture
  }

  override def fetchItemsByState(request: FetchByStateRequest): Future[FetchItemsResponse] = {
    logger.debug(s"Received fetch by `${request.state}` state request:")
    itemCol.source.find(Filters.eq("state", request.state.name))
      .take(request.limit).toListL.map(FetchItemsResponse.of).runToFuture
  }

}
