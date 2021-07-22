package monix.mini.platform.worker.grpc

import com.typesafe.scalalogging.LazyLogging
import io.grpc.{Server, ServerBuilder}
import monix.connect.mongodb.client.CollectionOperator
import monix.execution.Scheduler
import monix.mini.platform.protocol.Item
import monix.mini.platform.protocol.WorkerProtocolGrpc.WorkerProtocol
import monix.mini.platform.worker.config.WorkerConfig

class GrpcServer(config: WorkerConfig, itemColOp: CollectionOperator[Item])(implicit scheduler: Scheduler) extends LazyLogging {
  self =>

  private[this] var server: Server = null

  private def start(): Unit = {
    server = ServerBuilder.forPort(config.grpcServer.port)
      .addService(WorkerProtocol.bindService(new WorkerProtocolImpl(itemColOp), scheduler)).build.start
  }

  def shutDown(): Unit = {
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

}

