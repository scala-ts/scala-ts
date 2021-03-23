package io.github.scalats.sbttest.common

sealed trait Feature

object Feature {
  case object Foo extends Feature
  case object Bar extends Feature
}
