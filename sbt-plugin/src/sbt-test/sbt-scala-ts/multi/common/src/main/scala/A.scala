package io.github.scalats.sbttest.common

case class NamedFeature(
    name: String,
    feature: Feature)

sealed trait Category

object Category {
  case object Lorem extends Category
  case object Ipsum extends Category
}
