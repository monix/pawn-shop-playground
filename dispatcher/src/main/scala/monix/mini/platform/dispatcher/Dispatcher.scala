package monix.mini.platform.dispatcher

import com.typesafe.scalalogging.LazyLogging
import io.grpc.ManagedChannelBuilder
import cats.effect.concurrent.{ MVar, MVar2, Ref }
import monix.eval.Task
import monix.mini.platform.dispatcher.config.DispatcherConfig
import monix.mini.platform.dispatcher.domain.WorkerRef
import monix.mini.platform.dispatcher.kafka.KafkaPublisher
import monix.mini.platform.protocol._

import scala.concurrent.Future
import scala.util.Random.shuffle
import scalapb.GeneratedMessage
import cats.effect.ContextShift

class Dispatcher(config: DispatcherConfig, ref: Ref[Task, Seq[WorkerRef]])(implicit contextShift: ContextShift[Task]) extends LazyLogging {

  def createSlaveRef(workerInfo: WorkerInfo): WorkerRef = {
    val channel = ManagedChannelBuilder.forAddress(workerInfo.host, workerInfo.port).usePlaintext().build()
    val stub = WorkerProtocolGrpc.stub(channel)
    domain.WorkerRef(workerInfo.workerId, stub)
  }

  def publish[T <: GeneratedMessage](event: T, retries: Int)(implicit publisher: KafkaPublisher[T]): Task[Unit] =
    publisher.publish(event, retries).void

  def addNewSlave(workerInfo: WorkerInfo): Task[JoinResponse] = {
    logger.info(s"Adding new worker to the quorum: $workerInfo")
    val workerRef = createSlaveRef(workerInfo)
    for {
      _ <- ref.update(currentWorkers => currentWorkers.++(Seq(workerRef)))
      _ = logger.info(s"Filling Ref with ${workerRef}.")
    } yield (JoinResponse.JOINED)
  }

  def chooseSlave: Task[Option[WorkerRef]] = {
    for {
      slaves <- ref.get
      next <- Task.now {
        val slaveRef = slaves.headOption
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
    dispatch((slaveRef: WorkerRef) => slaveRef.stub.fetchItemsByName(fetchByNameRequest))
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

