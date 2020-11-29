package monix.mini.platform.slave

import io.grpc.{Server, ServerBuilder}
import monix.eval.Task
import monix.execution.{CancelableFuture, Scheduler}
import monix.mini.platform.master.config.SlaveConfig
import monix.mini.platform.protocol.{Document, FindRequest, InsertReply, InsertRequest, JoinReply, JoinRequest, JoinResponse}
import monix.mini.platform.protocol.MasterSlaveProtocolGrpc.MasterSlaveProtocol
import monix.mini.platform.protocol.SlaveProtocolGrpc.SlaveProtocol

import scala.concurrent.Future

class GrpcServer()(implicit config: SlaveConfig, scheduler: Scheduler) { self =>

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

  private class GreeterImpl extends SlaveProtocol {
    override def find(request: FindRequest): Future[Document] = {

    }

    override def insert(request: InsertRequest): Future[InsertReply] = {

    }
  }
}
