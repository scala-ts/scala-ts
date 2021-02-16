package io.github.scalats.sbttest

sealed trait Feature

object Feature {
  case object FooLure extends Feature
  case object BarNum extends Feature
}
