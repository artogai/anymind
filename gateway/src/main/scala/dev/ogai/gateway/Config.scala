package dev.ogai.gateway

import scala.concurrent.duration._

case class Config(port: Int, bootstrap: String, deliveryTimeout: FiniteDuration, responseTimeout: FiniteDuration)

object Config {
  def apply(): Config = Config(
    port = 8080,
    bootstrap = "kafka:9092",
    deliveryTimeout = 2.minutes,
    responseTimeout = 10.seconds,
  )
}
