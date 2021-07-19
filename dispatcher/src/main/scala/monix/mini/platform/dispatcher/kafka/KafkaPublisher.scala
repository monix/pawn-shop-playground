package monix.mini.platform.dispatcher.kafka

import com.typesafe.scalalogging.LazyLogging

import java.util.UUID
import monix.eval.Task
import monix.kafka.{KafkaProducer, KafkaProducerConfig}
import monix.mini.platform.dispatcher.kafkaIo
import org.apache.kafka.clients.producer.{ProducerRecord, RecordMetadata}
import scalapb.GeneratedMessage


class KafkaPublisher[T <: GeneratedMessage](kafkaTopicName: String)(implicit kafkaProducerConfig: KafkaProducerConfig) extends LazyLogging {

  val kafkaProducer: KafkaProducer[String, Array[Byte]] =
    KafkaProducer[String, Array[Byte]](kafkaProducerConfig, kafkaIo)

  def publish(event: T): Task[Option[RecordMetadata]] = {
    kafkaProducer.send(
      new ProducerRecord(kafkaTopicName, UUID.randomUUID().toString, event.toByteArray)
    )
  }

  def publish(event: T, retries: Int): Task[Option[RecordMetadata]] = {
    publish(event).onErrorHandleWith { ex =>
      if (retries > 0) publish(event, retries - 1)
      else {
        logger.error(s"Failed publishing event to topic ${kafkaTopicName}.", ex)
        Task.raiseError(ex)
      }
    }
  }


}

