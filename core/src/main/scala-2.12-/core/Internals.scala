package io.github.scalats.core

import scala.language.higherKinds

import scala.collection.{
  GenIterable,
  GenSet,
  GenTraversable,
  GenTraversableOnce,
  TraversableOnce
}
import scala.collection.generic.CanBuildFrom
import scala.collection.immutable.IndexedSeq
import scala.collection.mutable.Buffer

import scala.reflect.ClassTag

private[scalats] object Internals {

  @SuppressWarnings(Array("UnsafeTraversableMethods"))
  final class ListSet[T] private (
      private val values: Seq[T])
      extends TraversableOnce[T] {
    def iterator: Iterator[T] = values.iterator

    def headOption: Option[T] = values.headOption

    def tail: ListSet[T] = new ListSet(values.tail)

    @inline override def toList = values.toList

    def map[B](f: T => B): ListSet[B] =
      new ListSet(values.map(f).distinct)

    def flatMap[B](f: T => GenTraversableOnce[B]): ListSet[B] =
      new ListSet[B](values.flatMap(f).distinct)

    @inline override def nonEmpty: Boolean = values.nonEmpty

    def filter(f: T => Boolean): ListSet[T] =
      new ListSet[T](values filter f)

    def filterNot(f: T => Boolean): ListSet[T] =
      new ListSet[T](values filterNot f)

    @inline def contains(value: T): Boolean = values.contains(value)

    def collect[B](f: PartialFunction[T, B]): ListSet[B] =
      new ListSet[B](values.collect(f).distinct)

    def ++(other: GenTraversableOnce[T]): ListSet[T] =
      new ListSet[T]((values ++ other).distinct)

    def +(value: T): ListSet[T] = new ListSet[T]((values :+ value).distinct)

    def -(value: T): ListSet[T] = new ListSet[T](values.filterNot(_ == value))

    def zipWithIndex: ListSet[(T, Int)] = new ListSet(values.zipWithIndex)

    @inline def copyToArray[B >: T](
        xs: Array[B],
        start: Int,
        len: Int
      ): Unit =
      values.copyToArray(xs, start, len)

    @inline def exists(p: T => Boolean): Boolean = values.exists(p)

    @inline def find(p: T => Boolean): Option[T] = values.find(p)

    @inline def forall(p: T => Boolean): Boolean = values.forall(p)

    @inline def foreach[U](f: T => U): Unit = values.foreach(f)

    @inline def hasDefiniteSize: Boolean = values.hasDefiniteSize

    @inline def isEmpty: Boolean = values.isEmpty

    @inline def isTraversableAgain: Boolean = values.isTraversableAgain

    @inline def seq: TraversableOnce[T] = values

    @inline def toIterator: Iterator[T] = this.iterator

    @inline def toStream: Stream[T] = values.toStream

    @inline def toTraversable: Traversable[T] = values

    override def hashCode: Int = values.hashCode

    @SuppressWarnings(Array("ComparingUnrelatedTypes"))
    override def equals(that: Any): Boolean = that match {
      case other: ListSet[_] =>
        this.values == other.values

      case _ =>
        false
    }

    override def toString: String = values.mkString("ListSet[", ", ", "]")
  }

  object ListSet {
    def empty[T]: ListSet[T] = new ListSet(Seq.empty[T])

    def apply[T](values: T*): ListSet[T] = new ListSet(values.distinct)
  }
}
