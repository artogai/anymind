package dev.ogai.anymind

import scala.util.Try

import scalapb.{ GeneratedMessage, GeneratedMessageCompanion }

trait Format[A] {
  def encode(v: A): Array[Byte]
  def decode(bytes: Array[Byte]): Either[Throwable, A]

  def inmap[B](f: A => B)(g: B => A): Format[B] =
    Format.from(b => encode(g(b)), bytes => decode(bytes).map(f))
}

object Format {
  def apply[A: Format]: Format[A] =
    implicitly[Format[A]]

  def from[A](_encode: A => Array[Byte], _decode: Array[Byte] => Either[Throwable, A]): Format[A] =
    new Format[A] {
      override def encode(v: A): Array[Byte]                        = _encode(v)
      override def decode(bytes: Array[Byte]): Either[Throwable, A] = _decode(bytes)
    }

  def encode[A](value: A)(implicit f: Format[A]): Array[Byte] =
    f.encode(value)

  def decode[A](value: Array[Byte])(implicit f: Format[A]): Either[Throwable, A] =
    f.decode(value)

  implicit def protobufFormat[A <: GeneratedMessage](implicit gmc: GeneratedMessageCompanion[A]): Format[A] =
    new Format[A] {
      override def encode(v: A): Array[Byte]                        = gmc.toByteArray(v)
      override def decode(bytes: Array[Byte]): Either[Throwable, A] = Try(gmc.parseFrom(bytes)).toEither
    }
}
