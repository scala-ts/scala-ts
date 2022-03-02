package io.github.scalats.sbttest

import java.time.{ LocalDate, OffsetDateTime }

final class Name(val value: String) extends AnyVal

case class Bar(
    name: Name,
    aliases: Seq[Name],
    age: Int,
    amount: Option[BigInt],
    transports: Seq[Transport],
    updated: LocalDate,
    created: LocalDate)

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

object Constants {
  def code = 1
  val UnknownName = new Name("unknown")
  val defaultName = new Name("default")

  val list = Seq(code, 2)

  val dict = Map(
    "specific" -> List(UnknownName, defaultName, new Name("*")),
    "invalid" -> List(new Name("failed"))
  )

  val excluded = Seq("foo", "bar")

  val filtered = excluded ++ Seq("filtered")

  def names = List(UnknownName, defaultName) ++ Seq(new Name("test"))
}
