package monix.mini.platform.slave

import com.typesafe.scalalogging.LazyLogging
import io.grpc.{Server, ServerBuilder}
import monix.connect.mongodb.{MongoOp, MongoSource}
import monix.connect.redis.RedisSet
import monix.eval.Task
import monix.execution.Scheduler
import monix.mini.platform.protocol.SlaveProtocolGrpc.SlaveProtocol
import monix.mini.platform.slave.PersistanceRepository.{connection, operationsCol, transactionsCol, interactionsKey, branchesKey}
import monix.mini.platform.protocol._
import monix.mini.platform.slave.config.SlaveConfig
import com.mongodb.client.model.Filters

import scala.concurrent.Future

class GrpcServer(implicit config: SlaveConfig, scheduler: Scheduler) extends LazyLogging {
  self =>

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

    override def operation(operationEvent: OperationEvent): Future[EventResult] = {
      logger.info(s"Received operation event: ${operationEvent}")
      (for {
        _ <- MongoOp.insertOne(operationsCol, operationEvent.toEntity)
        _ <- RedisSet.sadd(branchesKey(operationEvent.client), operationEvent.branch)
      } yield EventResult.of(ResultStatus.INSERTED))
        .runToFuture
    }

    override def transaction(transactionEvent: TransactionEvent): Future[EventResult] = {
      logger.info(s"Received transaction event: ${transactionEvent}")

      (for {
        _ <- MongoOp.insertOne(transactionsCol, transactionEvent.toEntity)
        _ <- RedisSet.sadd(interactionsKey(transactionEvent.sender), transactionEvent.receiver)
      } yield EventResult.of(ResultStatus.INSERTED))
        .onErrorHandleWith { ex =>
          logger.error(s"Transaction failed with exception:", ex)
          Task.raiseError(ex)
        }
        .runToFuture
    }

    override def fetchAll(request: FetchRequest): Future[FetchAllReply] = {
      logger.debug(s"Received fetch request: ${request}")
      (for {
        transactions <- MongoSource.find(transactionsCol, Filters.eq("sender", request.client)).map(_.toProto).toListL
        operations <- MongoSource.find(operationsCol, Filters.eq("client", request.client)).map(_.toProto).toListL
        interactions <- RedisSet.smembers(interactionsKey(request.client)).toListL
        branches <- RedisSet.smembers(branchesKey(request.client)).toListL
      } yield FetchAllReply.of(transactions, operations, interactions, branches))
        .runToFuture
    }

    override def fetchTransactions(request: FetchRequest): Future[FetchTransactionsReply] = {
      logger.debug(s"Received fetch transactions request: ${request}")
      MongoSource.find(transactionsCol, Filters.eq("sender", request.client))
        .map(_.toProto)
        .toListL
        .map(FetchTransactionsReply.of)
        .runToFuture
    }

    override def fetchOperations(request: FetchRequest): Future[FetchOperationsReply] = {
      logger.debug(s"Received fetch operations request: ${request}")
      MongoSource.find(operationsCol, Filters.eq("client", request.client))
        .map(_.toProto)
        .toListL
        .map(FetchOperationsReply.of)
        .runToFuture
    }

    override def fetchInteractions(request: FetchRequest): Future[FetchInteractionsReply] = {
      logger.debug(s"Received fetch interactions request: ${request}")
      RedisSet.smembers(interactionsKey(request.client))
        .toListL
        .map(FetchInteractionsReply.of)
        .runToFuture
    }

    override def fetchBranches(request: FetchRequest): Future[FetchBranchesReply] = {
      RedisSet.smembers(branchesKey(request.client))
        .toListL
        .map(FetchBranchesReply.of)
        .runToFuture
    }
  }

}

