package dev.ogai.gateway

import akka.http.scaladsl.common.{ EntityStreamingSupport, JsonEntityStreamingSupport }
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import de.heikoseeberger.akkahttpcirce.FailFastCirceSupport._
import dev.ogai.anymind.model.Wallet.{ Amount, TimeRange }
import dev.ogai.gateway.JsonFormat._

object Routes {

  def wallet(walletClient: WalletClient): Route = {
    implicit val jsonStreamingSupport: JsonEntityStreamingSupport = EntityStreamingSupport.json()

    pathPrefix("wallet") {
      concat(
        pathEndOrSingleSlash {
          post {
            entity(as[Amount]) { amount =>
              if (amount.satoshi < 0)
                complete(StatusCodes.BadRequest)
              else
                onSuccess(walletClient.post(amount)) {
                _.complete(StatusCodes.OK)
              }
            }
          }
        },
        path("stats") {
          post {
            entity(as[TimeRange]) { range =>
              if ((range.endDatetime - range.startDatetime) / 60 <= 0)
                complete(walletClient.stats(range))
              else
                complete(StatusCodes.BadRequest)
            }
          }
        },
      )
    }
  }
}
