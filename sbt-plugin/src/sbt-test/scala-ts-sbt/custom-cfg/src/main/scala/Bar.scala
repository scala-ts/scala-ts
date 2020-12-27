package io.github.scalats.sbttest

case class Bar(
  name: String,
  age: Int,
  amount: Option[BigInt],
  transports: Seq[Transport],
  updated: java.time.LocalDate,
  created: java.time.LocalDate
)
