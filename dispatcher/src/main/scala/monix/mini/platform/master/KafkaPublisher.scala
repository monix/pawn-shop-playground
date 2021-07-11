package monix.mini.platform.master

import com.typesafe.scalalogging.LazyLogging
import java.util.UUID
import monix.eval.Task
import monix.kafka.KafkaProducer
import monix.mini.platform.config.DispatcherConfig.KafkaConfiguration
import org.apache.kafka.clients.producer.{ ProducerRecord, RecordMetadata }
import scalapb.GeneratedMessage

trait KafkaPublisher[T <: GeneratedMessage] extends LazyLogging {

  val kafka: KafkaConfiguration
  val topic: String
  val kafkaProducer: KafkaProducer[String, Array[Byte]] =
    KafkaProducer[String, Array[Byte]](kafka.producerConfig, kafkaIo)

  def publish(event: T): Task[Option[RecordMetadata]] = {
    kafkaProducer.send(
      new ProducerRecord(topic, UUID.randomUUID().toString, event.toByteArray)
    )
  }


}

