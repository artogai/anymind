package dev.ogai.gateway

import akka.http.scaladsl.Http
import akka.actor.ActorSystem
import scala.concurrent.ExecutionContext
import akka.http.scaladsl.server.Route

object Gateway extends App {

  implicit val system: ActorSystem  = ActorSystem("default")
  implicit val ec: ExecutionContext = ExecutionContext.global

  val cfg: Config                = Config()
  val kafkaClient: KafkaClient   = KafkaClient(cfg)
  val walletClient: WalletClient = WalletClient(kafkaClient)
  val routes: Route              = Routes.wallet(walletClient)

  Http()
    .newServerAt("localhost", cfg.port)
    .bindFlow(routes)
}
