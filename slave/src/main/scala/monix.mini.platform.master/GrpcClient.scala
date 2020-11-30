package monix.mini.platform.master

import io.grpc.ManagedChannelBuilder
import monix.eval.Task
import monix.mini.platform.master.SlaveApp.config
import monix.mini.platform.protocol.{ JoinReply, JoinRequest, MasterProtocolGrpc, SlaveInfo }

object GrpcClient {

  def sendJoinRequest: Task[JoinReply] = {
    //grpc client
    println("Sending join")
    val channel = ManagedChannelBuilder.forAddress(config.masterServer.host, config.masterServer.port).usePlaintext().build()
    val masterStub = MasterProtocolGrpc.stub(channel)
    val slaveInfo = SlaveInfo.of("slave1", config.grpcServer.host, config.grpcServer.port)
    Task.fromFuture(masterStub.join(JoinRequest.of(Some(slaveInfo))))
  }

}
