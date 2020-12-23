package io.github.scalats.core

import scala.collection.immutable.Set

final class TypeScriptField(
  val name: String,
  val flags: Set[TypeScriptField.Flag]) {

  override def toString = s"TypeScriptField${tupled.toString}"

  override def hashCode: Int = tupled.hashCode

  override def equals(that: Any): Boolean = that match {
    case other: TypeScriptField =>
      this.tupled == other.tupled

    case _ =>
      false
  }

  private lazy val tupled = name -> flags
}

object TypeScriptField {
  def apply(
    name: String,
    flags: Set[Flag] = Set.empty): TypeScriptField =
    new TypeScriptField(name, flags)

  final class Flag(val name: String) extends AnyVal

  /** Field on which `?` suffix can be applied (e.g. `name?: string`). */
  val omitable = new Flag("omitable")
}
