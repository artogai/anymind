package dev.ogai.wallet

import scala.concurrent.duration._

case class Config(bootstrap: String, deliveryTimeout: FiniteDuration, rocksdbPath: String)

object Config {
  def apply(): Config = Config(
    bootstrap = "kafka:9092",
    deliveryTimeout = 2.minutes,
    rocksdbPath = "rocksdb",
  )
}
