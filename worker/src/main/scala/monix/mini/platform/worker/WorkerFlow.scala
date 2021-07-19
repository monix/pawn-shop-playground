package monix.mini.platform.worker

import com.typesafe.scalalogging.LazyLogging
import monix.connect.mongodb.MongoSingle
import monix.reactive.Observable
import scalapb.GeneratedMessage

class WorkerFlow[T <: GeneratedMessage](kafkaConsumer: KafkaProtoConsumer[T], mongoSingle: MongoSingle[T]) extends LazyLogging {

  def run(committableErrorRetries: Int = 3): Observable[Unit] = {
    kafkaConsumer.startConsuming().mapEvalF {
      committableMessage =>
        val value = committableMessage.record.value()
        mongoSingle.insertOne(value)
          .onErrorHandle(logger.error("Error found inserting into mongo... ", _)) >>
          committableMessage.committableOffset
            .commitAsync()
            .onErrorRestart(committableErrorRetries)
            .onErrorHandle(logger.error("Error found committing offset... ", _))
    }
  }

}

object WorkerFlow {
  def apply[T <: GeneratedMessage](kafkaConsumer: KafkaProtoConsumer[T], mongoSingle: MongoSingle[T]): WorkerFlow[T] = {
    new WorkerFlow[T](kafkaConsumer, mongoSingle)
  }
}
