package monix.mini.platform.slave

import monix.mini.platform.protocol.OperationType
import org.bson.codecs.{Codec, DecoderContext, EncoderContext}
import org.bson.{BsonReader, BsonWriter}

class OperationTypeCodec extends Codec[OperationType] {

 override def encode (writer: BsonWriter, value: OperationType, encoderContext: EncoderContext): Unit = {
  writer.writeString(value.toString())
}

 override def decode (reader: BsonReader, decoderContext: DecoderContext): OperationType = {
   val value = reader.readString();
   OperationType.fromName(value).get
 }

 override def getEncoderClass: Class[OperationType] = classOf[OperationType]

}
