package monix.mini.platform.worker

import io.grpc.ManagedChannelBuilder
import monix.eval.Task
import WorkerApp.config
import com.typesafe.scalalogging.LazyLogging
import monix.mini.platform.protocol.{ JoinReply, JoinRequest, MasterProtocolGrpc, SlaveInfo }
import scala.concurrent.duration._

object GrpcClient extends LazyLogging {

  def sendJoinRequest(retries: Int): Task[JoinReply] = {
    //grpc client
    val channel = ManagedChannelBuilder.forAddress(config.dispatcherServer.host, config.dispatcherServer.port).usePlaintext().build()
    val masterStub = MasterProtocolGrpc.stub(channel)
    val slaveInfo = SlaveInfo.of(config.slaveId, config.grpcServer.host, config.grpcServer.port)
    Task.fromFuture(masterStub.join(JoinRequest.of(Some(slaveInfo))))
      .onErrorHandleWith { ex =>
        if (retries > 0) {
          logger.info(s"Remaining join retries ${retries}")
          sendJoinRequest(retries - 1).delayExecution(5.seconds)
        } else {
          logger.error(s"Reached maximum join request attemts with exception", ex)
          Task.raiseError(ex)
        }
      }
  }

}
