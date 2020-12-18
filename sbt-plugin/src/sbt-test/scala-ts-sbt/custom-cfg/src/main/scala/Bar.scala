package io.github.scalats.sbttest

case class Bar(
  name: String,
  age: Int,
  amount: Option[BigInt],
  updated: java.time.LocalDate,
  created: java.time.LocalDate
)
