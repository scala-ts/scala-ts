package org.scalats.core

import scala.collection.immutable.ListSet

private[core] object Internals {
  /** With predictable order (for emitter). */
  def list[T](set: ListSet[T]): List[T] = set.toList.reverse
}
