package monix.mini.platform.slave

import io.grpc.ManagedChannelBuilder
import monix.eval.Task
import monix.mini.platform.slave.SlaveApp.config
import monix.mini.platform.protocol.{JoinReply, JoinRequest, MasterProtocolGrpc, SlaveInfo}

object GrpcClient {

  def sendJoinRequest: Task[JoinReply] = {
    //grpc client
    val channel = ManagedChannelBuilder.forAddress(config.masterServer.host, config.masterServer.port).usePlaintext().build()
    val masterStub = MasterProtocolGrpc.stub(channel)
    val slaveInfo = SlaveInfo.of(config.slaveId, config.grpcServer.host, config.grpcServer.port)
    Task.fromFuture(masterStub.join(JoinRequest.of(Some(slaveInfo))))
  }

}
