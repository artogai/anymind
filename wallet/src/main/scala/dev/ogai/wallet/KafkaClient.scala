package dev.ogai.wallet

import scala.concurrent.Future
import scala.concurrent.duration._

import akka.Done
import akka.actor.ActorSystem
import akka.event.Logging
import akka.kafka.scaladsl.{ Consumer, Producer }
import akka.kafka.{ ConsumerSettings, ProducerSettings, Subscriptions }
import akka.stream.scaladsl.{ Flow, Keep, Sink, Source }
import dev.ogai.anymind.Topics
import dev.ogai.anymind.model.Wallet.{ Amount, StatsRequest, StatsResponse }
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.clients.producer.ProducerConfig
import org.apache.kafka.common.TopicPartition
import org.apache.kafka.common.serialization.{ ByteArrayDeserializer, ByteArraySerializer }

trait KafkaClient {
  def responsesSink(): Sink[StatsResponse, Future[Done]]
  def subscribeRequests(): Source[StatsRequest, Consumer.Control]
  def subscribeTransactions(offset: Long): Source[(Amount, Long), Consumer.Control]
}

object KafkaClient {
  def apply(cfg: Config)(implicit system: ActorSystem): KafkaClient = {
    val logger = Logging(system, this.getClass())

    new KafkaClient {
      override def responsesSink(): Sink[StatsResponse, Future[Done]] =
        Flow[StatsResponse]
          .map(Topics.statsResponses.encode)
          .toMat(Producer.plainSink(producerSettings(cfg)))(Keep.right)

      override def subscribeTransactions(offset: Long): Source[(Amount, Long), Consumer.Control] =
        Consumer
          .plainSource(
            consumerSettings(cfg, "transactions-consumers"),
            Subscriptions.assignmentWithOffset(new TopicPartition(Topics.transactions.name, 0), offset),
          )
          .mapConcat { rec =>
            Topics.transactions.decode(rec) match {
              case Left(ex) =>
                logger.error(s"Failed to decode response rec=$rec ex=$ex")
                None
              case Right(value) =>
                Some((value.value(), value.offset()))
            }
          }

      override def subscribeRequests(): Source[StatsRequest, Consumer.Control] =
        Consumer
          .plainSource(consumerSettings(cfg, "requests-consumers"), Subscriptions.topics(Topics.statsRequests.name))
          .mapConcat { rec =>
            Topics.statsRequests.decode(rec) match {
              case Left(ex) =>
                logger.error(s"Failed to decode response rec=$rec ex=$ex")
                None
              case Right(value) =>
                Some(value.value())
            }
          }
    }

  }

  private def producerSettings(cfg: Config)(implicit system: ActorSystem) =
    ProducerSettings(system, new ByteArraySerializer, new ByteArraySerializer)
      .withBootstrapServers(cfg.bootstrap)
      .withProperties(
        Map(
          ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG             -> true.toString,
          ProducerConfig.MAX_IN_FLIGHT_REQUESTS_PER_CONNECTION -> 5.toString,
          ProducerConfig.DELIVERY_TIMEOUT_MS_CONFIG            -> cfg.deliveryTimeout.toMillis.toString,
          ProducerConfig.RETRIES_CONFIG                        -> Int.MaxValue.toString,
          ProducerConfig.ACKS_CONFIG                           -> "all",
        )
      )

  private def consumerSettings(cfg: Config, groupId: String)(implicit system: ActorSystem) =
    ConsumerSettings(system, new ByteArrayDeserializer, new ByteArrayDeserializer)
      .withBootstrapServers(cfg.bootstrap)
      .withGroupId(groupId)
      .withProperties(
        Map(
          ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG -> false.toString,
          ConsumerConfig.AUTO_OFFSET_RESET_CONFIG  -> "latest",
        )
      )
      .withStopTimeout(Duration.Zero)

}
