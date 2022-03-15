package dev.ogai.gateway

import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.common.JsonEntityStreamingSupport
import akka.http.scaladsl.common.EntityStreamingSupport
import dev.ogai.anymind.model.Wallet.Amount
import JsonFormat._
import de.heikoseeberger.akkahttpcirce.FailFastCirceSupport._
import dev.ogai.anymind.model.Wallet.TimeRange
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Route

object Routes {

  def wallet(walletClient: WalletClient): Route = {
    implicit val jsonStreamingSupport: JsonEntityStreamingSupport = EntityStreamingSupport.json()

    path("wallet") {
      concat(
        post {
          entity(as[Amount]) { amount =>
            onSuccess(walletClient.post(amount)) {
              _.complete(StatusCodes.OK)
            }
          }
        },
        path("stats") {
          post {
            entity(as[TimeRange]) { range =>
              complete(walletClient.stats(range))
            }
          }
        },
      )
    }
  }
}
