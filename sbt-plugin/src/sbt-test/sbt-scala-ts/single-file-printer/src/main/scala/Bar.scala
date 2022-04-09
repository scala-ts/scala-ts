package io.github.scalats.sbttest

case class Bar(url: String)

object WeekDay extends Enumeration {
  type WeekDay = Value
  val Mon, Tue, Wed, Thu, Fri, Sat, Sun = Value
}

case class Lorem(
    val year: Int,
    weekday: WeekDay.WeekDay)
