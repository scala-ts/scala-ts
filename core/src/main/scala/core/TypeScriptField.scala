package io.github.scalats.core

import scala.collection.immutable.Set

case class TypeScriptField(
  name: String,
  flags: Set[TypeScriptField.Flag])

object TypeScriptField {
  final class Flag(val name: String) extends AnyVal

  val readonly = new Flag("readonly")

  /** Field on which `?` suffix can be applied (e.g. `name?: string`). */
  val omitable = new Flag("omitable")
}
