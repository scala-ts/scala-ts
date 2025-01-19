package io.github.scalats.sbttest

import enumeratum._

sealed abstract class State(override val entryName: String) extends EnumEntry

object State extends Enum[State] {
  val values = findValues

  case object Alabama extends State("AL")
  case object Alaska extends State("AK")

  val cities = Map[State, Set[String]](
    Alaska -> Set("Juneau", "Anchorage"),
    Alabama -> Set("Birmingham")
  )
}
