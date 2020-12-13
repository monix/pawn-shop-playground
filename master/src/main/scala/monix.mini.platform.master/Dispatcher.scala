package monix.mini.platform.master

import com.typesafe.scalalogging.LazyLogging
import io.grpc.ManagedChannelBuilder
import monix.catnap.MVar
import monix.eval.Task
import monix.mini.platform.config.MasterConfig
import monix.mini.platform.master.typeclass.Fetch
import monix.mini.platform.protocol.{ EventResult, FetchRequest, JoinResponse, OperationEvent, ResultStatus, SlaveInfo, SlaveProtocolGrpc, TransactionEvent }

import scala.util.Random.shuffle
import scala.concurrent.Future

class Dispatcher(implicit config: MasterConfig) extends LazyLogging {

  private val slaves: Task[MVar[Task, Seq[SlaveRef]]] = MVar[Task].of[Seq[SlaveRef]](Seq.empty).memoize

  def createSlaveRef(slaveInfo: SlaveInfo): SlaveRef = {
    val channel = ManagedChannelBuilder.forAddress(slaveInfo.host, slaveInfo.port).usePlaintext().build()
    val stub = SlaveProtocolGrpc.stub(channel)
    SlaveRef(slaveInfo.slaveId, stub)
  }

  def addNewSlave(slaveInfo: SlaveInfo): Task[JoinResponse] = {
    val slaveRef = createSlaveRef(slaveInfo)
    for {
      mvar <- slaves
      seq <- mvar.take
      joinResponse <- {
        if (seq.size < 3) {
          val updatedSlaves = Seq(slaveRef)
          logger.info(s"Filling mvar with ${slaveRef}, is empty: ${mvar.isEmpty}")
          mvar.put(updatedSlaves).map(_ => JoinResponse.JOINED)
        } else {
          Task.now(JoinResponse.REJECTED)
        }
      }
    } yield joinResponse
  }

  def chooseSlave: Task[Option[SlaveRef]] = {
    for {
      mvar <- slaves
      slaves <- mvar.read
      next <- Task.now {
        val slaveRef = shuffle(slaves).headOption
        logger.info(s"Choosen slave ref ${slaveRef} from the available list of slaves: ${slaves}")
        slaveRef
      }
    } yield next
  }

  val sendTransaction = (slaveRef: SlaveRef, transactionEvent: TransactionEvent) => slaveRef.stub.transaction(transactionEvent)
  def dispatch(transactionEvent: TransactionEvent): Task[EventResult] = {
    logger.debug("Dispatching transaction")
    dispatch[TransactionEvent, EventResult](transactionEvent, sendTransaction, EventResult.of(ResultStatus.FAILED))
  }

  val sendOperation = (slaveRef: SlaveRef, operationEvent: OperationEvent) => slaveRef.stub.operation(operationEvent)
  def dispatch(operationEvent: OperationEvent): Task[EventResult] = {
    logger.debug("Dispatching transaction")
    dispatch(operationEvent, sendOperation, EventResult.of(ResultStatus.FAILED))
  }

  def dispatch[Proto, View](fetchRequest: FetchRequest)(implicit F: Fetch[Proto, View]): Task[View] = {
    logger.debug("Dispatching fetch branches")
    dispatch[FetchRequest, Proto](fetchRequest, F.send, F.defaultInstance).map(F.toView(_))
  }

  def dispatch[E, R](event: E, send: (SlaveRef, E) => Future[R], fallback: R): Task[R] = {
    for {
      maybeSlaveRef <- chooseSlave
      joinReq <- {
        maybeSlaveRef match {
          case Some(slaveRef) => Task.fromFuture(send(slaveRef, event))
          case None => Task.now(fallback)
        }
      }
    } yield joinReq
  }

}

