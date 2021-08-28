package io.github.scalats.sbttest

import java.time.{ LocalDate, OffsetDateTime }

final class Name(val value: String) extends AnyVal

case class Bar(
  name: Name,
  age: Int,
  amount: Option[BigInt],
  transports: Seq[Transport],
  updated: LocalDate,
  created: LocalDate
)

case class Foo(
  id: Long,
  namesp: (Int, String),
  row: Tuple3[String, Transport, OffsetDateTime],
  score: Either[Int, String],
  rates: Map[String, Float])

case class NotSupportedClassAsTypeArgs[T](
  name: String,
  value: T)

case class NotSupportedAsNotSupportedField(
  notSupportedClassAsTypeArgs: NotSupportedClassAsTypeArgs[Float])
