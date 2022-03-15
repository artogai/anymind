package dev.ogai.gateway

import io.circe.syntax._
import dev.ogai.anymind.model.Wallet.TimeRange
import dev.ogai.anymind.model.Wallet.Amount
import java.time.OffsetDateTime
import java.time.Instant
import java.time.ZoneId
import io.circe.Decoder
import io.circe.Encoder
import io.circe.Json

object JsonFormat {
  implicit val amountDecoder: Decoder[Amount] =
    json =>
      for {
        datetime <- json.get[OffsetDateTime]("datetime")
        amount   <- json.get[Double]("amount")
      } yield Amount(datetime.toEpochSecond(), amount)

  implicit val amountEncoder: Encoder[Amount] = {
    val utc = ZoneId.of("UTC")
    amount =>
      Json.obj(
        "datetime" -> OffsetDateTime.ofInstant(Instant.ofEpochSecond(amount.datetime), utc).asJson,
        "amount"   -> amount.amount.asJson,
      )
  }

  implicit val timeRangeDecoder: Decoder[TimeRange] =
    json =>
      for {
        startDateTime <- json.get[OffsetDateTime]("startDatetime")
        endDateTime   <- json.get[OffsetDateTime]("endDateTime")
      } yield TimeRange(startDateTime.toEpochSecond(), endDateTime.toEpochSecond())
}
