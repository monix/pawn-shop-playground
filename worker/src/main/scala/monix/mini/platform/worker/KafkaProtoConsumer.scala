package monix.mini.platform.worker

import monix.kafka.{ CommittableMessage, Deserializer, KafkaConsumerConfig, KafkaConsumerObservable }
import monix.kafka.Deserializer.{ forStrings, fromKafkaDeserializer }
import monix.mini.platform.protocol.Item
import monix.reactive.Observable
import scalapb.GeneratedEnum

import scalapb.{ GeneratedMessage, GeneratedMessageCompanion }
import org.apache.kafka.common.serialization.{ Deserializer => KafkaDeserializer }

import java.util

class KafkaProtoConsumer[T <: GeneratedMessage](topic: String)(implicit kafkaConsumerConfig: KafkaConsumerConfig, gMCompanion: GeneratedMessageCompanion[T]) {

  private def deserializeMessage(message: Array[Byte]): T = gMCompanion.parseFrom(message)

  implicit val kafkaDeserializar: Deserializer[T] =
    fromKafkaDeserializer(
      new KafkaDeserializer[T] {
        override def configure(configs: util.Map[String, _], isKey: Boolean): Unit = ()
        override def deserialize(topic: String, data: Array[Byte]): T = deserializeMessage(data)
        override def close(): Unit = ()
      })

  def startConsuming(): Observable[CommittableMessage[String, T]] =
    KafkaConsumerObservable.manualCommit(cfg = kafkaConsumerConfig, topics = List(topic))

}
