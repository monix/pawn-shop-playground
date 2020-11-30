package monix.mini.platform.master

import com.typesafe.scalalogging.LazyLogging
import io.grpc.{Server, ServerBuilder}
import monix.eval.Task
import monix.execution.{CancelableFuture, Scheduler}
import monix.mini.platform.config.MasterConfig
import monix.mini.platform.protocol.MasterProtocolGrpc.MasterProtocol
import monix.mini.platform.protocol.{JoinReply, JoinRequest, JoinResponse}

class GrpcServer(dispatcher: Dispatcher)(implicit config: MasterConfig, scheduler: Scheduler) extends LazyLogging { self =>

  private[this] var server: Server = null

  private def start(): Unit = {
    server = ServerBuilder.forPort(config.grpcServer.port)
      .addService(MasterProtocol.bindService(new MasterImpl, scheduler)).build.start
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

  private class MasterImpl extends MasterProtocol {
    override def join(req: JoinRequest): CancelableFuture[JoinReply] = {
      logger.info(s"Join Request received with slave info: ${req.slaveInfo}")
      val joinResponse = req.slaveInfo match {
        case Some(slaveInfo) => dispatcher.addNewSlave(slaveInfo)
        case None => Task.now(JoinResponse.REJECTED)
      }
      joinResponse.map(JoinReply.of).runToFuture
    }
  }
}
