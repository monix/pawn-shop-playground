package monix.mini.platform.master.kafka

import com.typesafe.scalalogging.LazyLogging
import java.util.UUID
import monix.eval.Task
import monix.kafka.KafkaProducer
import monix.mini.platform.config.DispatcherConfig.KafkaTopicConfig
import monix.mini.platform.master.kafkaIo
import org.apache.kafka.clients.producer.{ ProducerRecord, RecordMetadata }
import scalapb.GeneratedMessage


class KafkaPublisher[T <: GeneratedMessage](kafkaTopicConfig: KafkaTopicConfig) extends LazyLogging {

  val kafkaProducer: KafkaProducer[String, Array[Byte]] =
    KafkaProducer[String, Array[Byte]](kafkaTopicConfig.producerConfig, kafkaIo)

  def publish(event: T): Task[Option[RecordMetadata]] = {
    kafkaProducer.send(
      new ProducerRecord(kafkaTopicConfig.topicName, UUID.randomUUID().toString, event.toByteArray)
    )
  }

  def publish(event: T, retries: Int): Task[Option[RecordMetadata]] = {
    publish(event).onErrorHandleWith { ex =>
      if (retries > 0) publish(event, retries - 1)
      else {
        logger.error(s"Failed publishing event to topic ${kafkaTopicConfig.topicName}.", ex)
        Task.raiseError(ex)
      }
    }
  }


}

