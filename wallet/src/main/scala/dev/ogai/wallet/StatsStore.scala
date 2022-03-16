package dev.ogai.wallet

import java.nio.ByteBuffer
import java.nio.file.{ Files, Paths }

import scala.collection.mutable
import scala.concurrent.{ ExecutionContext, Future }
import scala.jdk.CollectionConverters._
import scala.util.{ Failure, Success, Try, Using }

import akka.NotUsed
import akka.stream.scaladsl.Source
import dev.ogai.anymind.model.Wallet.{ Amount, StatsRequest, StatsResponse }
import org.rocksdb.{
  ColumnFamilyDescriptor, ColumnFamilyHandle, ColumnFamilyOptions, DBOptions, ReadOptions, RocksDB, WriteBatch,
  WriteOptions,
}
import org.slf4j.LoggerFactory

trait StatsStore {
  def put(amount: Amount, offset: Long): Future[Unit]
  def get(req: StatsRequest): Source[StatsResponse, NotUsed]
  def lastState: Future[(Long, Long)]
  def close(): Unit
}

object StatsStore {
  def rocksdb(path: String, blockingCtx: ExecutionContext) = {
    val logger = LoggerFactory.getLogger(this.getClass())

    Files.createDirectories(Paths.get(path))

    val defaultCfd     = new ColumnFamilyDescriptor(RocksDB.DEFAULT_COLUMN_FAMILY, new ColumnFamilyOptions())
    val walletStatsCfd = new ColumnFamilyDescriptor("wallet_stats".getBytes, new ColumnFamilyOptions())

    val cfds = mutable.ListBuffer[ColumnFamilyDescriptor](defaultCfd, walletStatsCfd)
    val cfhs = mutable.ListBuffer[ColumnFamilyHandle]()

    val dbOpts = new DBOptions()
      .setCreateIfMissing(true)
      .setCreateMissingColumnFamilies(true)

    val db = RocksDB.open(dbOpts, path, cfds.asJava, cfhs.asJava)

    val walletStatsCfh = cfhs(1)

    val emptyBytes = Array.emptyByteArray

    new StatsStore {
      override def put(amount: Amount, offset: Long): Future[Unit] =
        Future {
          Using.resources(new WriteOptions(), new WriteBatch()) { (writeOpts, writeBatch) =>
            val hour           = (amount.datetime / (60 * 60)).toInt
            val satoshi        = amount.satoshi
            val hourBytes      = ByteBuffer.allocate(4).putInt(hour).array()
            val amountBytes    = ByteBuffer.allocate(8).putLong(satoshi).array()
            val lastStateBytes = ByteBuffer.allocate(16).putLong(offset).putLong(satoshi).array()

            writeBatch.put(walletStatsCfh, hourBytes, amountBytes)
            writeBatch.put(emptyBytes, lastStateBytes)

            db.write(writeOpts, writeBatch)
          }
        }(blockingCtx)

      override def get(req: StatsRequest): Source[StatsResponse, NotUsed] = {
        val startHour = {
          val hour   = (req.range.startDatetime / (60 * 60)).toInt
          val second = (req.range.startDatetime % (60 * 60)).toInt

          if (second == 0) hour else hour + 1
        }

        val endHour = (req.range.endDatetime / (60 * 60)).toInt

        val it = Iterator.from(startHour).takeWhile(_ <= endHour)

        Source
          .fromIterator(() => it)
          .mapAsync(1) { hour =>
            Future {
              Using.resource(new ReadOptions) { readOpts =>
                val hourBytes    = ByteBuffer.allocate(4).putInt(hour).array()
                val satoshiBytes = Array.ofDim[Byte](8)

                db.get(walletStatsCfh, readOpts, hourBytes, satoshiBytes)

                val satoshi = ByteBuffer.wrap(satoshiBytes).getLong()

                StatsResponse(req.id, Amount(hour.toLong * 60 * 60, satoshi), hour == endHour)
              }
            }(blockingCtx)
          }
          .scan((0L, Option.empty[StatsResponse])) { case ((prevSatoshi, _), resp) =>
            if (resp.amount.satoshi == 0)
              (prevSatoshi, Some(resp.copy(amount = resp.amount.copy(satoshi = prevSatoshi))))
            else
              (resp.amount.satoshi, Some(resp))
          }
          .collect { case (_, Some(resp)) => resp }
      }

      override def lastState: Future[(Long, Long)] =
        Future {
          Using.resource(new ReadOptions()) { readOpts =>
            val lastStateBytes = Array.ofDim[Byte](16)
            db.get(readOpts, emptyBytes, lastStateBytes)
            val buff = ByteBuffer.wrap(lastStateBytes)
            (buff.getLong(), buff.getLong())
          }
        }(blockingCtx)

      override def close(): Unit = {
        Try(cfhs.foreach(_.close())) match {
          case Failure(ex) => logger.error(s"Failed to close column families: cfhs=$cfhs ex=$ex")
          case Success(_)  => ()
        }

        Try(db.close()) match {
          case Failure(ex) => logger.error(s"Failed to close rocksdb: ex=$ex")
          case Success(_)  => ()
        }

        Try(dbOpts.close()) match {
          case Failure(ex) => logger.error(s"Failed to close options: ex=$ex")
          case Success(_)  => ()
        }
      }
    }
  }

}
