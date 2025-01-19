package io.github.scalats.sbttest

sealed abstract class State(val entryName: String)

object State {
  case object Alabama extends State("AL")
  case object Alaska extends State("AK")

  val cities = Map[State, Set[String]](
    Alaska -> Set("Anchorage", "Juneau"),
    Alabama -> Set("Birmingham")
  )
}
