package io.github.scalats.sbttest

case class Name(value: String) extends AnyVal

case class Bar(
    name: Name,
    age: Int,
    amount: Option[BigInt],
    transports: Seq[Transport],
    updated: java.time.LocalDate,
    created: java.time.LocalDate)

object Constants {
  val DefaultName = Name("default")

  val excluded = Seq("foo", "bar")

  val filtered = excluded ++ Seq("filtered")

  def list = List(DefaultName) ++ Seq(Name("test"))

  val seqOfMap = Seq(
    Map(Name("lorem") -> "lorem", DefaultName -> "ipsum"),
    Map(Name("dolor") -> "value")
  )
}
