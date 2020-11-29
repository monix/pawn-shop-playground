package monix.mini.platform.master

import io.grpc.ManagedChannelBuilder
import monix.catnap.MVar
import monix.eval.Task
import monix.mini.platform.config.MasterConfig
import monix.mini.platform.protocol.{JoinResponse, SlaveInfo, SlaveProtocolGrpc}

class Dispatcher(implicit config: MasterConfig) {

  private val slaves: Task[MVar[Task, Iterator[SlaveRef]]] = MVar.of(LazyList.empty)

  def createSlaveRef(slaveInfo: SlaveInfo): SlaveRef = {
    val channel = ManagedChannelBuilder.forAddress(slaveInfo.host, slaveInfo.port.toInt).build()
    val stub = SlaveProtocolGrpc.stub(channel)
    SlaveRef(slaveInfo.slaveId, stub)
  }

  def addNewSlave(slaveInfo: SlaveInfo): Task[JoinResponse] = {
    val slaveRef = createSlaveRef(slaveInfo)
    for {
      mvar <- slaves
      seq <- mvar.read
      joinResponse <- {
        if(seq.size < 3) {
          val updatedSlaves = seq.++(Seq(slaveRef))
          mvar.put(updatedSlaves).as(JoinResponse.JOINED)
        } else {
          Task.now(JoinResponse.REJECTED)
        }
      }
    } yield joinResponse
  }

  def nextSlave: Task[SlaveRef] = {
    for {
      mvar <- slaves
      lazyList <- mvar.read
      next <- Task.now(lazyList.next)
    } yield next
  }




}
