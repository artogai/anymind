package dev.ogai.gateway

import scala.concurrent.duration.Duration
import scala.concurrent.{ ExecutionContext, Future }

import akka.actor.ActorSystem
import akka.event.Logging
import akka.kafka.scaladsl.{ Consumer, SendProducer }
import akka.kafka.{ ConsumerSettings, ProducerSettings, Subscriptions }
import akka.stream.scaladsl.Source
import dev.ogai.anymind.Topics
import dev.ogai.anymind.model.Wallet.{ Amount, StatsRequest, StatsResponse }
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.clients.producer.ProducerConfig
import org.apache.kafka.common.serialization.{ ByteArrayDeserializer, ByteArraySerializer }

trait KafkaClient {
  def send(amount: Amount): Future[Unit]
  def send(statsRequest: StatsRequest): Future[Unit]
  def subscribeResponses(): Source[StatsResponse, Consumer.Control]
  def close(): Future[Unit]
}

object KafkaClient {

  def apply(cfg: Config)(implicit system: ActorSystem, ec: ExecutionContext): KafkaClient = {
    val logger   = Logging(system, this.getClass())
    val producer = SendProducer(producerSettings(cfg))

    new KafkaClient {

      override def send(amount: Amount): Future[Unit] =
        producer.send(Topics.transactions.encode(amount)).map(_ => ())

      override def send(statsRequest: StatsRequest): Future[Unit] =
        producer.send(Topics.statsRequests.encode(statsRequest)).map(_ => ())

      override def subscribeResponses(): Source[StatsResponse, Consumer.Control] =
        Consumer
          .plainSource(consumerSettings(cfg, "responses-consumer"), Subscriptions.topics(Topics.statsResponses.name))
          .mapConcat { rec =>
            Topics.statsResponses.decode(rec) match {
              case Left(ex) =>
                logger.error(s"Failed to decode response rec=$rec ex=$ex")
                None
              case Right(value) =>
                Some(value.value())
            }
          }

      override def close(): Future[Unit] = producer.close().map(_ => ())
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
