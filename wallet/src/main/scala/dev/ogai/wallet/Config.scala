package dev.ogai.wallet

case class Config(bootstrap: String)

object Config {
  def apply(): Config = Config(
    bootstrap = "kafka:9092"
  )
}
