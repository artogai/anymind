package dev.ogai.gateway

import java.time.{ Instant, OffsetDateTime, ZoneId }

import dev.ogai.anymind.model.Wallet.{ Amount, TimeRange }
import io.circe.syntax._
import io.circe.{ Decoder, Encoder, Json }

object JsonFormat {

  implicit val amountDecoder: Decoder[Amount] =
    json =>
      for {
        datetime <- json.get[OffsetDateTime]("datetime")
        amount   <- json.get[BigDecimal]("amount")
        satoshi = toSatoshi(amount)
      } yield Amount(datetime.toEpochSecond(), satoshi)

  implicit val amountEncoder: Encoder[Amount] = {
    val utc = ZoneId.of("UTC")
    amount =>
      Json.obj(
        "datetime" -> OffsetDateTime.ofInstant(Instant.ofEpochSecond(amount.datetime), utc).asJson,
        "amount"   -> fromSatoshi(amount.satoshi).asJson,
      )
  }

  implicit val timeRangeDecoder: Decoder[TimeRange] =
    json =>
      for {
        startDateTime <- json.get[OffsetDateTime]("startDatetime")
        endDateTime   <- json.get[OffsetDateTime]("endDateTime")
      } yield TimeRange(startDateTime.toEpochSecond(), endDateTime.toEpochSecond())

  private val satoshiInBtc = 100000000
  private def toSatoshi(amount: BigDecimal): Long =
    (amount * satoshiInBtc).toLongExact
  private def fromSatoshi(satoshi: Long): BigDecimal =
    BigDecimal(satoshi) / satoshiInBtc

}
