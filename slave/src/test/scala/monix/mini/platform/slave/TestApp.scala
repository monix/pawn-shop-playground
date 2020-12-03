package monix.mini.platform.slave

import monix.connect.mongodb.{MongoDb, MongoOp}
import monix.mini.platform.protocol.{OperationEvent, OperationType, TransactionEvent}
import monix.mini.platform.slave.PersistanceRepository.{codecRegistry, db, transactionsCol}
import monix.execution.Scheduler.Implicits.global
import org.bson.codecs.configuration.{CodecRegistries, CodecRegistry}

object TestApp extends App {

 //MongoOp.insertOne(transactionsCol, TransactionEventView("1", "2", "3", 4)).runSyncUnsafe()
 //MongoOp.insertOne(operationsCol1, OperationEventView("1", "2", 3, "4", OperationType.WITHDRAW)).runSyncUnsafe()


}
