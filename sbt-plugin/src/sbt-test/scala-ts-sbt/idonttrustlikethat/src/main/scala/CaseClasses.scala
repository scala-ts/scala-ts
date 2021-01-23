package io.github.scalats.sbttest

import java.time.{ LocalDate, OffsetDateTime }

case class Bar(
  name: String,
  age: Int,
  amount: Option[BigInt],
  transports: Seq[Transport],
  updated: LocalDate,
  created: LocalDate
)

case class Foo(
  id: Long,
  namespace: (Int, String),
  row: Tuple3[String, Transport, OffsetDateTime])

case class NotSupportedClassAsTypeArgs[T](
  name: String,
  value: T)

case class NotSupportedAsNotSupportedField(
  notSupportedClassAsTypeArgs: NotSupportedClassAsTypeArgs[Float])
