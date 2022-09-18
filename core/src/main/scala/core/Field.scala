package io.github.scalats.core

import scala.collection.immutable.Set

final class Field(
    val name: String,
    val flags: Set[Field.Flag]) {

  override def toString = s"Field${tupled.toString}"

  override def hashCode: Int = tupled.hashCode

  override def equals(that: Any): Boolean = that match {
    case other: Field =>
      this.tupled == other.tupled

    case _ =>
      false
  }

  private lazy val tupled = name -> flags
}

object Field {

  def apply(
      name: String,
      flags: Set[Flag] = Set.empty
    ): Field =
    new Field(name, flags)

  final class Flag(val name: String) extends AnyVal

  /** Field on which `?` suffix can be applied (e.g. `name?: string`). */
  val omitable = new Flag("omitable")
}
