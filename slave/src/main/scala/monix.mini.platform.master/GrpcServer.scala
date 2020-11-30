package monix.mini.platform.slave

import com.mongodb.client.model.Filters
import com.mongodb.reactivestreams.client.MongoCollection
import io.grpc.{ Server, ServerBuilder }
import monix.connect.mongodb.{ MongoOp, MongoSource }
import monix.eval.Task
import monix.execution.Scheduler
import monix.mini.platform.master.config.SlaveConfig
import monix.mini.platform.protocol.{ Document, FindRequest, FindReply, InsertReply, InsertRequest, InsertResult }
import monix.mini.platform.protocol.SlaveProtocolGrpc.SlaveProtocol

import scala.concurrent.Future

class GrpcServer(col: MongoCollection[Document])(implicit config: SlaveConfig, scheduler: Scheduler) { self =>

  private[this] var server: Server = null

  private def start(): Unit = {
    server = ServerBuilder.forPort(config.grpcServer.port)
      .addService(SlaveProtocol.bindService(new SlaveProtocolImpl, scheduler)).build.start
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

  private class SlaveProtocolImpl extends SlaveProtocol {
    override def insert(request: InsertRequest): Future[InsertReply] = {
      val t = request.document match {
        case Some(doc) => MongoOp.insertOne(col, doc).map(_ => InsertResult.INSERTED)
        case None => Task.now(InsertResult.FAILED)
      }
      t.map(InsertReply.of).runToFuture
    }

    override def find(request: FindRequest): Future[FindReply] = {
      val filter = Filters.eq("key", request.key)
      MongoSource.find(col, filter)
        .firstOptionL
        .map {
          case someDoc: Some[Document] => FindReply.of(someDoc)
          case None => FindReply.of(Option.empty)
        }.runToFuture
    }

  }
}
