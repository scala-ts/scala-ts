package io.github.scalats.core

import scala.collection.immutable.ListSet

private[scalats] object Internals {

  /** With predictable order (for emitter). */
  @inline def list[T](set: ListSet[T]): List[T] = set.toList
}
