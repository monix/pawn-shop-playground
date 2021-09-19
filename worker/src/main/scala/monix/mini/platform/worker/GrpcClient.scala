package monix.mini.platform.worker

import io.grpc.ManagedChannelBuilder
import monix.eval.Task
import com.typesafe.scalalogging.LazyLogging
import monix.mini.platform.protocol.{DispatcherProtocolGrpc, JoinReply, JoinRequest, WorkerInfo}
import monix.mini.platform.worker.config.WorkerConfig
import monix.mini.platform.worker.config.WorkerConfig.GrpcConfig

import scala.concurrent.duration._

class GrpcClient(slaveId: String, grpcConfig: GrpcConfig) extends LazyLogging {

  val channel = ManagedChannelBuilder.forAddress(grpcConfig.server.host, grpcConfig.server.port).usePlaintext().build()
  val masterStub = DispatcherProtocolGrpc.stub(channel)
  val slaveInfo = WorkerInfo.of(slaveId, grpcConfig.server.host, grpcConfig.server.port)

  def sendJoinRequest(retries: Int, backoffDelay: FiniteDuration = 5.seconds): Task[JoinReply] = {
    //grpc client
    Task.fromFuture(masterStub.join(JoinRequest.of(Some(slaveInfo))))
      .onErrorHandleWith { ex =>
        if (retries > 0) {
          logger.info(s"Remaining join retries ${retries}")
          sendJoinRequest(retries - 1).delayExecution(backoffDelay)
        } else {
          logger.error(s"Reached maximum join request attempts with exception", ex)
          Task.raiseError(ex)
        }
      }
  }

}

object GrpcClient {
  def apply(slaveId: String, grpcConfig: GrpcConfig) = new GrpcClient(config)
}
