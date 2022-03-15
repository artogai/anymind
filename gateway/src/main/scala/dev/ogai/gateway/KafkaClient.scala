package dev.ogai.gateway

import cats.syntax.functor._
import dev.ogai.anymind.model.Wallet.StatsRequest
import dev.ogai.anymind.model.Wallet.Amount
import scala.concurrent.Future
import akka.stream.scaladsl.Source
import akka.NotUsed
import dev.ogai.anymind.model.Wallet.StatsResponse
import org.apache.kafka.clients.consumer.ConsumerConfig
import akka.kafka.ConsumerSettings
import org.apache.kafka.common.serialization.ByteArrayDeserializer
import akka.actor.ActorSystem
import scala.concurrent.duration._
import org.apache.kafka.common.serialization.ByteArraySerializer
import akka.kafka.scaladsl.Consumer
import akka.kafka.Subscriptions
import akka.kafka.ProducerSettings
import org.apache.kafka.clients.producer.ProducerConfig
import akka.kafka.scaladsl.SendProducer
import dev.ogai.anymind.Topics
import scala.concurrent.ExecutionContext
import akka.event.Logging

trait KafkaClient {
  def send(amount: Amount): Future[Unit]
  def send(statsRequest: StatsRequest): Future[Unit]
  def subscribeResponses(): Source[StatsResponse, NotUsed]
  def close(): Future[Unit]
}

object KafkaClient {

  def apply(cfg: Config)(implicit system: ActorSystem, ec: ExecutionContext): KafkaClient = {
    val producer               = kafkaProducer(cfg)
    val (consumerCtrl, source) = kafkaConsumer(cfg).preMaterialize()

    new KafkaClient {
      override def send(amount: Amount): Future[Unit] =
        producer.send(Topics.transactions.encode(amount)).void

      override def send(statsRequest: StatsRequest): Future[Unit] =
        producer.send(Topics.statsRequests.encode(statsRequest)).void

      override def subscribeResponses(): Source[StatsResponse, NotUsed] =
        source

      override def close(): Future[Unit] = {
        val c1 = producer.close()
        val c2 = consumerCtrl.shutdown()

        c1.zip(c2).void
      }
    }
  }

  private def kafkaProducer(cfg: Config)(implicit system: ActorSystem): SendProducer[Array[Byte], Array[Byte]] = {
    val settings =
      ProducerSettings(system, new ByteArraySerializer, new ByteArraySerializer)
        .withBootstrapServers(cfg.bootstrap)
        .withProperties(
          Map(
            ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG             -> true.toString,
            ProducerConfig.MAX_IN_FLIGHT_REQUESTS_PER_CONNECTION -> 5.toString,
            ProducerConfig.DELIVERY_TIMEOUT_MS_CONFIG            -> cfg.requestTimeout.toMillis.toString,
            ProducerConfig.RETRIES_CONFIG                        -> Int.MaxValue.toString,
            ProducerConfig.ACKS_CONFIG                           -> "all",
          )
        )

    SendProducer(settings)
  }

  private def kafkaConsumer(cfg: Config)(implicit system: ActorSystem): Source[StatsResponse, Consumer.Control] = {
    val logger = Logging(system, this.getClass())
    val settings =
      ConsumerSettings(system, new ByteArrayDeserializer, new ByteArrayDeserializer)
        .withBootstrapServers(cfg.bootstrap)
        .withGroupId("responses-consumer")
        .withProperties(
          Map(
            ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG      -> true.toString,
            ConsumerConfig.AUTO_COMMIT_INTERVAL_MS_CONFIG -> 5.seconds.toMillis.toString,
            ConsumerConfig.AUTO_OFFSET_RESET_CONFIG       -> "latest",
          )
        )

    Consumer
      .plainSource(settings, Subscriptions.topics(Topics.statsResponses.name))
      .mapConcat { rec =>
        Topics.statsResponses.decode(rec) match {
          case Left(ex) =>
            logger.error(s"Failed to decode response rec=$rec ex=$ex")
            None
          case Right(value) =>
            Some(value.value())
        }
      }
  }
}
