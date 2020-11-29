package monix.mini.platform.master

import io.grpc.{ Server, ServerBuilder }
import monix.eval.Task
import monix.execution.{ CancelableFuture, Scheduler }
import monix.mini.platform.config.MasterConfig
import monix.mini.platform.protocol.{ JoinReply, JoinRequest, JoinResponse }
import monix.mini.platform.protocol.MasterSlaveProtocolGrpc.MasterSlaveProtocol

class GrpcServer(implicit config: MasterConfig, scheduler: Scheduler) { self =>

  private[this] var server: Server = null

  private def start(): Unit = {
    server = ServerBuilder.forPort(config.grpcServer.port)
      .addService(MasterSlaveProtocol.bindService(new GreeterImpl, scheduler)).build.start
    sys.addShutdownHook {
      System.err.println("*** shutting down gRPC server since JVM is shutting down")
      self.stop()
      System.err.println("*** server shut down")
    }
  }

  private def stop(): Unit = {
    if (server != null) {
      server.shutdown()
    }
  }

  def blockUntilShutdown(): Unit = {
    self.start()
    if (server != null) {
      server.awaitTermination()
    }
  }

  private class GreeterImpl extends MasterSlaveProtocol {
    override def join(req: JoinRequest): CancelableFuture[JoinReply] = {
      Task.now(JoinReply(JoinResponse.JOINED)).runToFuture
    }
  }
}
