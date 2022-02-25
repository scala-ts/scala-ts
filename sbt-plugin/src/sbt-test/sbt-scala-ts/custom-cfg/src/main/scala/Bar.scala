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
}
