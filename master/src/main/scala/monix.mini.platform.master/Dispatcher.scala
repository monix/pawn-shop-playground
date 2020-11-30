package monix.mini.platform.master

import com.typesafe.scalalogging.LazyLogging
import io.grpc.ManagedChannelBuilder
import monix.catnap.MVar
import monix.eval.Task
import monix.mini.platform.config.MasterConfig
import monix.mini.platform.protocol.{FindReply, FindRequest, JoinResponse, SlaveInfo, SlaveProtocolGrpc}

class Dispatcher(implicit config: MasterConfig) extends LazyLogging{

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
      lazyList <- mvar.read
      next <- Task.now {
        val slaveRef = lazyList.headOption //todo
        logger.info(s"Choosen slave ref ${slaveRef} from the available list of: ${lazyList}")
        slaveRef
      }
    } yield next
  }

  def sendFindRequest(key: String): Task[FindReply] = {
    val findRequest = FindRequest.of(None, key)
    for {
      maybeSlaveRef <- chooseSlave
      joinReq <- {
        maybeSlaveRef match {
          case Some(slaveRef) => Task.fromFuture(slaveRef.stub.find(findRequest))
          case None => Task.now(FindReply.defaultInstance)
        }
      }
    } yield joinReq
  }
}
