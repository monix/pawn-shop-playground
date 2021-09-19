package monix.mini.platform.worker.grpc

import com.typesafe.scalalogging.LazyLogging
import io.grpc.{Server, ServerBuilder}
import monix.connect.mongodb.client.CollectionOperator
import monix.execution.Scheduler
import monix.mini.platform.protocol.{Buy, Item, Pawn, Sell}
import monix.mini.platform.protocol.WorkerProtocolGrpc.WorkerProtocol
import monix.mini.platform.worker.config.WorkerConfig
import monix.mini.platform.worker.config.WorkerConfig.GrpcConfig

class GrpcServer(grpcConfig: GrpcConfig, itemColOp: CollectionOperator[Item], buyColOp: CollectionOperator[Buy],
  sellColOp: CollectionOperator[Sell], pawnColOp: CollectionOperator[Pawn])(implicit scheduler: Scheduler) extends LazyLogging {
  self =>

  private[this] var server: Server = null

  private def start(): Unit = {
    server = ServerBuilder.forPort(grpcConfig.server.port)
      .addService(WorkerProtocol.bindService(new WorkerProtocolImpl(itemColOp, buyColOp, sellColOp, pawnColOp), scheduler)).build.start
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

