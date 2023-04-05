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
      extends GenTraversableOnce[T] {
    def iterator: Iterator[T] = values.iterator

    def headOption: Option[T] = values.headOption

    @inline override def toList = values.toList

    def map[B](f: T => B): ListSet[B] =
      new ListSet(values.map(f).distinct)

    def flatMap[B](f: T => GenTraversableOnce[B]): ListSet[B] =
      new ListSet[B](values.flatMap(f).distinct)

    @inline override def nonEmpty: Boolean = values.nonEmpty

    def filterNot(f: T => Boolean): ListSet[T] =
      new ListSet[T](values filterNot f)

    @inline def contains(value: T): Boolean = values.contains(value)

    def collect[B](f: PartialFunction[T, B]): ListSet[B] =
      new ListSet[B](values.collect(f).distinct)

    def ++(other: GenTraversableOnce[T]): ListSet[T] =
      new ListSet[T]((values ++ other).distinct)

    def +(value: T): ListSet[T] = new ListSet[T]((values :+ value).distinct)

    def -(value: T): ListSet[T] = new ListSet[T](values.filterNot(_ == value))

    @inline def :\[B](z: B)(op: (T, B) => B): B = values.:\(z)(op)

    @inline def /:[B](z: B)(op: (B, T) => B): B = values./:(z)(op)

    def zipWithIndex: ListSet[(T, Int)] = new ListSet(values.zipWithIndex)

    @inline def aggregate[B](
        z: => B
      )(seqop: (B, T) => B,
        combop: (B, B) => B
      ): B = values.aggregate[B](z)(seqop, combop)

    @inline def copyToArray[B >: T](
        xs: Array[B],
        start: Int,
        len: Int
      ): Unit =
      values.copyToArray(xs, start, len)

    @inline def copyToArray[B >: T](xs: Array[B], start: Int): Unit =
      values.copyToArray(xs, start)

    @inline def copyToArray[B >: T](
        xs: Array[B]
      ): Unit = values.copyToArray(xs)

    @inline def count(p: T => Boolean): Int = values.count(p)

    @inline def exists(p: T => Boolean): Boolean = values.exists(p)

    @inline def find(p: T => Boolean): Option[T] = values.find(p)

    @inline def fold[A1 >: T](z: A1)(op: (A1, A1) => A1): A1 =
      values.fold(z)(op)

    @inline def foldLeft[B](z: B)(op: (B, T) => B): B =
      values.foldLeft(z)(op)

    @inline def foldRight[B](z: B)(op: (T, B) => B): B =
      values.foldRight(z)(op)

    @inline def forall(p: T => Boolean): Boolean = values.forall(p)

    @inline def foreach[U](f: T => U): Unit = values.foreach(f)

    @inline def hasDefiniteSize: Boolean = values.hasDefiniteSize

    @inline def isEmpty: Boolean = values.isEmpty

    @inline def isTraversableAgain: Boolean = values.isTraversableAgain

    @inline def max[A1 >: T](
        implicit
        ord: Ordering[A1]
      ): T =
      values.max[A1](ord)

    @inline def maxBy[B](
        f: T => B
      )(implicit
        cmp: Ordering[B]
      ): T =
      values.maxBy(f)(cmp)

    @inline def min[A1 >: T](
        implicit
        ord: Ordering[A1]
      ): T =
      values.min(ord)

    @inline def minBy[B](
        f: T => B
      )(implicit
        cmp: Ordering[B]
      ): T =
      values.minBy(f)(cmp)

    @inline def mkString: String = values.mkString

    @inline def mkString(sep: String): String = values.mkString(sep)

    @inline def mkString(
        start: String,
        sep: String,
        end: String
      ): String =
      values.mkString(start, sep, end)

    @inline def product[A1 >: T](
        implicit
        num: Numeric[A1]
      ): A1 =
      values.product(num)

    @inline def reduce[A1 >: T](
        op: (A1, A1) => A1
      ): A1 = values.reduce(op)

    @inline def reduceLeftOption[B >: T](op: (B, T) => B): Option[B] =
      values.reduceLeftOption(op)

    @inline def reduceOption[A1 >: T](op: (A1, A1) => A1): Option[A1] =
      values.reduceOption(op)

    @inline def reduceRight[B >: T](
        op: (T, B) => B
      ): B = values.reduceRight(op)

    @inline def reduceRightOption[B >: T](op: (T, B) => B): Option[B] =
      values.reduceRightOption(op)

    @inline def seq: TraversableOnce[T] = values

    @inline def size: Int = values.size

    @inline def sum[A1 >: T](
        implicit
        num: Numeric[A1]
      ): A1 = values.sum(num)

    @inline def to[Col[_]](
        implicit
        cbf: CanBuildFrom[Nothing, T, Col[T]]
      ): Col[T] =
      values.to(cbf)

    @inline def toArray[A1 >: T](
        implicit
        ev: ClassTag[A1]
      ): Array[A1] =
      values.toArray[A1](ev)

    @inline def toBuffer[A1 >: T]: Buffer[A1] = values.toBuffer[A1]

    @inline def toIndexedSeq: IndexedSeq[T] = values.toIndexedSeq

    @inline def toIterable: GenIterable[T] = values

    @inline def toIterator: Iterator[T] = this.iterator

    @inline def toMap[K, V](
        implicit
        ev: <:<[T, (K, V)]
      ): scala.collection.GenMap[K, V] = ???

    @inline def toSeq: Seq[T] = values

    @inline def toSet[A1 >: T]: GenSet[A1] = values.toSet

    @inline def toStream: Stream[T] = values.toStream

    @inline def toTraversable: GenTraversable[T] = values

    @inline def toVector: Vector[T] = values.toVector

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
