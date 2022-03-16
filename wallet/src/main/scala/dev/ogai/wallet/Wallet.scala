package dev.ogai.wallet

import java.util.concurrent.Executors

import scala.concurrent.ExecutionContext
import scala.util.{ Failure, Success }

import akka.actor.ActorSystem
import akka.event.{ Logging, LoggingAdapter }
import akka.stream.scaladsl.{ Keep, Sink }
import dev.ogai.anymind.{ Topics, Utils }

object Wallet extends App {

  implicit val system: ActorSystem  = ActorSystem("default")
  implicit val ec: ExecutionContext = system.dispatcher
  val blockingEc: ExecutionContext  = ExecutionContext.fromExecutor(Executors.newCachedThreadPool())

  val logger: LoggingAdapter   = Logging(system, this.getClass())
  val cfg: Config              = Config()
  val kafkaClient: KafkaClient = KafkaClient(cfg)
  val statsStore: StatsStore   = StatsStore.rocksdb(cfg.rocksdbPath, blockingEc)

  Utils.createTopic(cfg.bootstrap, Topics.transactions)
  Utils.createTopic(cfg.bootstrap, Topics.statsRequests)
  Utils.createTopic(cfg.bootstrap, Topics.statsResponses)

  statsStore
    .lastState
    .map { case (lastOffset, lastSatoshiCnt) =>
      logger.info(s"Starting from state offset=$lastOffset lastSatoshiCnt=$lastSatoshiCnt")

      kafkaClient
        .subscribeTransactions(lastOffset)
        .drop(if (lastOffset == 0) 0 else 1)
        .foldAsync(lastSatoshiCnt) { case (satoshiCnt, (amount, offset)) =>
          val nextSatoshiCnt = amount.satoshi + satoshiCnt
          statsStore
            .put(amount.copy(satoshi = nextSatoshiCnt), offset)
            .map(_ => nextSatoshiCnt)
        }
        .toMat(Sink.ignore)(Keep.both)
        .run()
    }
    .flatMap { case (_, transactionsDone) =>
      val (_, requestsDone) =
        kafkaClient
          .subscribeRequests()
          .flatMapMerge(Int.MaxValue, req => statsStore.get(req))
          .toMat(kafkaClient.responsesSink())(Keep.both)
          .run()

      requestsDone.zip(transactionsDone)
    }
    .onComplete {
      case Success(_) =>
        logger.warning("App stopped")
        statsStore.close()
        system.terminate()
      case Failure(ex) =>
        logger.error(s"App failed with ex ${ex.printStackTrace()}");
        statsStore.close()
        system.terminate()
    }
}
