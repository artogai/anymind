package dev.ogai.anymind

import scala.jdk.CollectionConverters._

import org.apache.kafka.clients.admin.NewTopic
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.apache.kafka.clients.producer.ProducerRecord

case class Topic[V](
    name: String,
    partitions: Int,
    config: Map[String, String],
    encode: V => ProducerRecord[Array[Byte], Array[Byte]],
    decode: ConsumerRecord[Array[Byte], Array[Byte]] => Either[Throwable, ConsumerRecord[Array[Byte], V]],
) {
  def asNewTopic: NewTopic = {
    val nt = new NewTopic(name, partitions, 1.toShort)
    if (config.nonEmpty) nt.configs(config.asJava) else nt
  }
}

object Topic {
  def apply[V: Format](
      name: String,
      partitions: Int,
      config: Map[String, String],
  ): Topic[V] =
    new Topic[V](
      name = name,
      partitions = partitions,
      config = config,
      encode = (v: V) => new ProducerRecord[Array[Byte], Array[Byte]](name, Format.encode(v)),
      decode = (rec: ConsumerRecord[Array[Byte], Array[Byte]]) =>
        Format
          .decode[V](rec.value())
          .map { value =>
            new ConsumerRecord[Array[Byte], V](
              rec.topic(),
              rec.partition(),
              rec.offset(),
              null,
              value,
            )
          },
    )
}
