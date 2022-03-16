package dev.ogai.gateway

import scala.concurrent.ExecutionContext

import akka.actor.ActorSystem
import akka.event.Logging
import akka.http.scaladsl.Http
import akka.http.scaladsl.server.Route
import dev.ogai.anymind.{ Topics, Utils }

object Gateway extends App {

  implicit val system: ActorSystem  = ActorSystem("default")
  implicit val ec: ExecutionContext = system.dispatcher
  val logger                        = Logging(system, this.getClass())

  val cfg: Config = Config()

  logger.info("Creating topics...")
  Utils.createTopic(cfg.bootstrap, Topics.transactions)
  Utils.createTopic(cfg.bootstrap, Topics.statsRequests)
  Utils.createTopic(cfg.bootstrap, Topics.statsResponses)
  logger.info("Topics created...")

  val kafkaClient: KafkaClient   = KafkaClient(cfg)
  val walletClient: WalletClient = WalletClient(kafkaClient, cfg)
  val routes: Route              = Routes.wallet(walletClient)

  logger.info(s"Binding server to port ${cfg.port}...")
  Http()
    .newServerAt("0.0.0.0", cfg.port)
    .bindFlow(routes)
    .map { _ =>
      logger.info(s"Server binded")
    }
}
