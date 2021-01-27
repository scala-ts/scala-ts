package io.github.scalats.sbttest

import enumeratum._

sealed trait Greeting extends EnumEntry

object Greeting extends Enum[Greeting] {
  val values = findValues

  case object Hello extends Greeting
  case object GoodBye extends Greeting
  case object Hi extends Greeting
  case object Bye extends Greeting

  case class Whatever(word: String) extends Greeting
}
