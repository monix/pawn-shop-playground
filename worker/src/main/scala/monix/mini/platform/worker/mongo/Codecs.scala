package monix.mini.platform.worker.mongo

import monix.mini.platform.protocol.{Category, State}
import org.bson.codecs.configuration.{CodecProvider, CodecRegistry}
import org.bson.codecs.{Codec, DecoderContext, EncoderContext}
import org.bson.{BsonReader, BsonWriter}
import scalapb.UnknownFieldSet

object Codecs {

  val protoCodecProvider: CodecProvider = new CodecProvider {
    override def get[T](clazz: Class[T], registry: CodecRegistry): Codec[T] = {
      if (Category.values.exists(clazz == _.getClass)) CategoryCodec.asInstanceOf[Codec[T]]
      else if (State.values.exists(clazz == _.getClass)) StateCodec.asInstanceOf[Codec[T]]
      else if (clazz == UnknownFieldSet.empty.getClass) UnknownFieldSetCodec.asInstanceOf[Codec[T]]
      else null
    }
  }

  object CategoryCodec extends Codec[Category] {
    override def encode(writer: BsonWriter, value: Category, encoderContext: EncoderContext): Unit = writer.writeString(value.toString())
    override def decode(reader: BsonReader, decoderContext: DecoderContext): Category = Category.fromName(reader.readString()).getOrElse(Category.Motor)
    override def getEncoderClass: Class[Category] = classOf[Category]
  }

  object StateCodec extends Codec[State] {
    override def encode(writer: BsonWriter, value: State, encoderContext: EncoderContext): Unit = writer.writeString(value.toString())
    override def decode(reader: BsonReader, decoderContext: DecoderContext): State = State.fromName(reader.readString()).getOrElse(State.Good)
    override def getEncoderClass: Class[State] = classOf[State]
  }

  object UnknownFieldSetCodec extends Codec[UnknownFieldSet] {
    override def encode(writer: BsonWriter, value: UnknownFieldSet, encoderContext: EncoderContext): Unit = writer.writeUndefined()
    override def decode(reader: BsonReader, decoderContext: DecoderContext): UnknownFieldSet = {
      reader.readUndefined()
      UnknownFieldSet.empty
    }
    override def getEncoderClass: Class[UnknownFieldSet] = classOf[UnknownFieldSet]
  }

}
