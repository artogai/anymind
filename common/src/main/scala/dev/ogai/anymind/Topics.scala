package dev.ogai.anymind

import dev.ogai.anymind.model.Wallet.{ Amount, StatsRequest, StatsResponse }

object Topics {

  /** In real use-case we will have multiple wallets which are evenly distributed by multiple partitions so different
    * wallet requests can be processed in parallel
    */

  val transactions: Topic[Amount] =
    Topic(
      name = "transactions",
      partitions = 1,
      config = Map(
        "cleanup.policy"  -> "delete",
        "retention.bytes" -> "-1",
        "retention.ms"    -> "-1",// store transactions indefinitely
      ),
    )

  val statsRequests: Topic[StatsRequest] =
    Topic(
      name = "stats_requests",
      partitions = 1,
      config = Map(
        "cleanup.policy"  -> "delete",
        "retention.bytes" -> "-1",
        "retention.ms"    -> (24 * 60 * 60 * 1000).toString,// store requests for 24 hours
      ),
    )

  val statsResponses: Topic[StatsResponse] =
    Topic(
      name = "stats_responses",
      partitions = 1,
      config = Map(
        "cleanup.policy"  -> "delete",
        "retention.bytes" -> "-1",
        "retention.ms"    -> (24 * 60 * 60 * 1000).toString,// store responses for 24 hours
      ),
    )
}
