package monix.mini.platform.worker

import com.typesafe.scalalogging.LazyLogging
import monix.connect.mongodb.MongoSingle
import monix.reactive.Observable
import scalapb.GeneratedMessage

class InboundFlow[T <: GeneratedMessage](kafkaConsumer: KafkaProtoConsumer[T], mongoSingle: MongoSingle[T]) extends LazyLogging {

  def run(committableErrorRetries: Int = 3): Observable[Unit] = {
    kafkaConsumer.startConsuming().mapEvalF {
      committableMessage =>
        val value = committableMessage.record.value()
        logger.info(s"Processing message from topic `${kafkaConsumer.topic}`.")
        mongoSingle.insertOne(value)
          .onErrorHandle(logger.error("Error found inserting into mongo... ", _)) >>
          committableMessage.committableOffset
          .commitAsync()
          .onErrorRestart(committableErrorRetries)
          .onErrorHandle(logger.error("Error found committing offset... ", _))
    }
  }

}

object InboundFlow {
  def apply[T <: GeneratedMessage](kafkaConsumer: KafkaProtoConsumer[T], mongoSingle: MongoSingle[T]): InboundFlow[T] = {
    new InboundFlow[T](kafkaConsumer, mongoSingle)
  }
}
