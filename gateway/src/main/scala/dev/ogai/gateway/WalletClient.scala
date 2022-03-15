package dev.ogai.gateway

import java.util.UUID

import scala.collection.mutable
import scala.concurrent.Future

import akka.actor.{Actor, ActorRef, ActorSystem, Props}
import akka.pattern.pipe
import akka.stream.scaladsl.Source
import akka.stream.{CompletionStrategy, OverflowStrategy}
import akka.{Done, NotUsed}
import dev.ogai.anymind.model.Wallet.{Amount, StatsRequest, StatsResponse, TimeRange}
import scala.util.Failure
import akka.event.Logging
import scala.concurrent.ExecutionContext

trait WalletClient {
  def post(amount: Amount): Future[Unit]
  def stats(range: TimeRange): Source[Amount, NotUsed]
}

object WalletClient {

  def apply(kafka: KafkaClient)(implicit system: ActorSystem, ec: ExecutionContext): WalletClient = {
    val statsReqRouter = system.actorOf(Props(classOf[WalletStats.Router], kafka))
    val logger = Logging(system, this.getClass())

    kafka
      .subscribeResponses()
      .map { resp =>
        statsReqRouter ! WalletStats.Response(resp)
      }
      .run()
      .onComplete {
        case Failure(ex) =>
          logger.error(s"Responses subscription failed with $ex")
          system.terminate()
        case _ =>
          logger.error(s"Responses subscription stopped")
          system.terminate()
      }

    new WalletClient {
      override def post(amount: Amount): Future[Unit] =
        kafka.send(amount)

      override def stats(range: TimeRange): Source[Amount, NotUsed] = {
        val completionMatcher: PartialFunction[Any, CompletionStrategy] = { case Done =>
          CompletionStrategy.draining
        }

        val failureMatcher: PartialFunction[Any, Throwable] = { case WalletStats.Error(_, ex) =>
          ex
        }

        val (reqActor, reqSource) = Source
          .actorRef[Amount](
            completionMatcher = completionMatcher,
            failureMatcher = failureMatcher,
            bufferSize = 100,
            overflowStrategy = OverflowStrategy.fail,
          )
          .preMaterialize()

        val req = StatsRequest(UUID.randomUUID().toString(), range)
        statsReqRouter ! WalletStats.Request(req, reqActor)

        reqSource
      }
    }
  }
}

object WalletStats {
  sealed trait Event
  final case class Request(value: StatsRequest, actor: ActorRef) extends Event
  final case class Response(value: StatsResponse)                extends Event
  final case class Error(id: String, ex: Throwable)              extends Event
  final case object Sent                                         extends Event

  class Router(kafkaClient: KafkaClient) extends Actor {
    import context.dispatcher

    val requests = mutable.Map[String, ActorRef]()

    override def receive: Receive = {
      case Request(req, actor) =>
        requests += (req.id -> actor)
        kafkaClient.send(req).pipeTo(self)
        ()
      case Response(resp) =>
        if (resp.isFinal) {
          requests
            .remove(resp.id)
            .foreach { ref =>
              ref ! resp
              ref ! Done
            }
        } else {
          requests
            .get(resp.id)
            .foreach { ref =>
              ref ! resp
            }
        }
      case err: Error =>
        requests
          .remove(err.id)
          .foreach { ref =>
            ref ! err
          }
      case () =>
        ()
    }
  }
}
