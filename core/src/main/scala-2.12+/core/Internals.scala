package io.github.scalats.core

import scala.collection.immutable.{ ListSet => LS }

private[scalats] object Internals {
  type ListSet[T] = LS[T]

  val ListSet = LS
}
