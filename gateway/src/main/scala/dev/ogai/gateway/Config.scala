package dev.ogai.gateway

import scala.concurrent.duration._

case class Config(port: Int, bootstrap: String, requestTimeout: FiniteDuration)

object Config {
  def apply(): Config = Config(
    port = 8080,
    bootstrap = "kafka:9092",
    requestTimeout = 10.seconds,
  )
}
