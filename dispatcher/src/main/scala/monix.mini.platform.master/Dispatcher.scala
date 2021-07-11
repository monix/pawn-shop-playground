package monix.mini.platform.master

import com.typesafe.scalalogging.LazyLogging
import io.grpc.ManagedChannelBuilder
import monix.catnap.MVar
import monix.eval.Task
import monix.mini.platform.config.DispatcherConfig
import monix.mini.platform.protocol.{ FetchByCategoryRequest, FetchByIdRequest, FetchByNameRequest, FetchByStateRequest, FetchItemResponse, FetchItemsResponse, Item, JoinResponse, WorkerInfo, WorkerProtocolGrpc }
import scala.util.Random.shuffle
import scala.concurrent.Future
import scalapb.GeneratedMessage

class Dispatcher(config: DispatcherConfig) extends LazyLogging {

  private val workers: Task[MVar[Task, Seq[WorkerRef]]] = MVar[Task].of[Seq[WorkerRef]](Seq.empty).memoize

  def createSlaveRef(workerInfo: WorkerInfo): WorkerRef = {
    val channel = ManagedChannelBuilder.forAddress(workerInfo.host, workerInfo.port).usePlaintext().build()
    val stub = WorkerProtocolGrpc.stub(channel)
    WorkerRef(workerInfo.workerId, stub)
  }

  def publish[T <: GeneratedMessage](event: T, retries: Int)(implicit publisher: KafkaPublisher[T]): Task[Unit] = {
    publisher.publish(event).onErrorHandleWith { ex =>
      if (retries > 0) publish(event, retries - 1)
      else {
        logger.error(s"Failed publishing event to topic ${publisher.topic}.", ex)
        Task.raiseError(ex).void
      }
    }.void
  }

  def addNewSlave(workerInfo: WorkerInfo): Task[JoinResponse] = {
    val workerRef = createSlaveRef(workerInfo)
    for {
      mvar <- workers
      seq <- mvar.take
      joinResponse <- {
        if (seq.size < 10) {
          val updatedSlaves = Seq(workerRef)
          logger.info(s"Filling MVar with ${workerRef}, is empty: ${mvar.isEmpty}")
          mvar.put(updatedSlaves).map(_ => JoinResponse.JOINED)
        } else {
          Task.now(JoinResponse.REJECTED)
        }
      }
    } yield joinResponse
  }

  def chooseSlave: Task[Option[WorkerRef]] = {
    for {
      mvar <- workers
      slaves <- mvar.read
      next <- Task.now {
        val slaveRef = shuffle(slaves).headOption
        logger.info(s"Chosen slave ref ${slaveRef} from the available list of workers: ${slaves}")
        slaveRef
      }
    } yield next
  }

  def fetchItem(fetchByIdRequest: FetchByIdRequest): Task[FetchItemResponse] = {
    logger.debug("Dispatching transaction")
    dispatch((slaveRef: WorkerRef) => slaveRef.stub.fetchItemById(fetchByIdRequest))
  }

  def fetchItem(fetchByNameRequest: FetchByNameRequest): Task[FetchItemsResponse] = {
    logger.debug("Dispatching transaction")
    dispatch((slaveRef: WorkerRef) => slaveRef.stub.fetchItemsByName(fetchByNameRequest)
    )
  }

  def fetchItem(fetchByCategoryRequest: FetchByCategoryRequest): Task[FetchItemsResponse] = {
    logger.debug("Dispatching FetchByCategoryRequest")
    dispatch((slaveRef: WorkerRef) =>
        slaveRef.stub.fetchItemsByCategory(fetchByCategoryRequest))
  }

  def fetchItem(fetchByStateRequest: FetchByStateRequest): Task[FetchItemsResponse] = {
    logger.debug("Dispatching transaction")
    dispatch((slaveRef: WorkerRef) => slaveRef.stub.fetchItemsByState(fetchByStateRequest))
  }

  def dispatch[R](fetch: WorkerRef => Future[R]): Task[R] = {
    for {
      maybeSlaveRef <- chooseSlave
      response <- {
        maybeSlaveRef match {
          case Some(slaveRef) => Task.fromFuture(fetch(slaveRef))
          case None => Task.raiseError(new IllegalStateException("No slave available."))
        }
      }
    } yield response
  }


}

