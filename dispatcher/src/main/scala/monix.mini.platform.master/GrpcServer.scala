package monix.mini.platform.master

import com.typesafe.scalalogging.LazyLogging
import io.grpc.protobuf.services.ProtoReflectionService
import io.grpc.{ Server, ServerBuilder }
import monix.eval.Task
import monix.execution.{ CancelableFuture, Scheduler }
import monix.mini.platform.config.DispatcherConfig
import monix.mini.platform.protocol.DispatcherProtocolGrpc.DispatcherProtocol
import monix.mini.platform.protocol.{ JoinReply, JoinRequest, JoinResponse }
import monix.mini.platform.protocol.JoinResponse

class GrpcServer(dispatcher: Dispatcher)(implicit config: DispatcherConfig, scheduler: Scheduler) extends LazyLogging { self =>

  private[this] var server: Server = null

  logger.info(s"Starting grpc server on endpoint: ${config.grpcServer.endPoint}")

  private def start(): Unit = {
    server = ServerBuilder.forPort(config.grpcServer.port)
      .addService(DispatcherProtocol.bindService(new DispatcherImpl, scheduler))
      .addService(ProtoReflectionService.newInstance())
      .build.start
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

  private class DispatcherImpl extends DispatcherProtocol {
    override def join(req: JoinRequest): CancelableFuture[JoinReply] = {
      logger.info(s"Join Request received with slave info: ${req.workerInfo}")
      val joinResponse = req.workerInfo match {
        case Some(workerInfo) => dispatcher.addNewSlave(workerInfo)
        case None => Task.now(JoinResponse.REJECTED)
      }
      joinResponse.map(JoinReply.of).runToFuture
    }
  }
}
