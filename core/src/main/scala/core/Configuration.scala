package org.scalats.core

// TODO: Move files to src/main/scala
// TODO: option as space-lift Option
// TODO: Per-type options: nullable, fieldNaming, emitCodecs, prelude

/**
 * Created by Milosz on 09.12.2016.
 */
final class Configuration(
  val emitInterfaces: Boolean,
  val emitClasses: Boolean,
  val optionToNullable: Boolean,
  val optionToUndefined: Boolean,
  val prependIPrefix: Boolean,
  val prependEnclosingClassNames: Boolean = false,
  val typescriptIndent: String,
  val emitCodecs: Boolean,
  val fieldNaming: FieldNaming) {

  private[core] def copy(
    emitInterfaces: Boolean = this.emitInterfaces,
    emitClasses: Boolean = this.emitClasses,
    optionToNullable: Boolean = this.optionToNullable,
    optionToUndefined: Boolean = this.optionToUndefined,
    prependIPrefix: Boolean = this.prependIPrefix,
    prependEnclosingClassNames: Boolean = this.prependEnclosingClassNames,
    typescriptIndent: String = this.typescriptIndent,
    emitCodecs: Boolean = this.emitCodecs,
    fieldNaming: FieldNaming = this.fieldNaming): Configuration =
    new Configuration(
      emitInterfaces,
      emitClasses,
      optionToNullable,
      optionToUndefined,
      prependIPrefix,
      prependEnclosingClassNames,
      typescriptIndent,
      emitCodecs,
      fieldNaming)

  override def equals(that: Any): Boolean = that match {
    case other: Configuration => tupled == other.tupled

    case _ => false
  }

  override def hashCode: Int = tupled.hashCode

  override def toString = tupled.toString

  private lazy val tupled = Tuple9(
    emitInterfaces,
    emitClasses,
    optionToNullable,
    optionToUndefined,
    prependIPrefix,
    prependEnclosingClassNames,
    typescriptIndent,
    emitCodecs,
    fieldNaming)
}

// TODO: nullable as function setting (gathering optionToNullable/optionToUndefined)
// TODO: option as space-lift/fp-ts Option
// TODO: prelude: String

// TODO: Per-type options: nullable, fieldNaming, emitCodecs, prelude (by annotation on such type?)

object Configuration {
  import scala.xml._

  def apply(
    emitInterfaces: Boolean = true,
    emitClasses: Boolean = false,
    optionToNullable: Boolean = true,
    optionToUndefined: Boolean = false,
    prependIPrefix: Boolean = true,
    prependEnclosingClassNames: Boolean = true,
    typescriptIndent: String = "\t",
    emitCodecs: Boolean = true,
    fieldNaming: FieldNaming = FieldNaming.Identity): Configuration =
    new Configuration(
      emitInterfaces,
      emitClasses,
      optionToNullable,
      optionToUndefined,
      prependIPrefix,
      prependEnclosingClassNames,
      typescriptIndent,
      emitCodecs,
      fieldNaming)

  def load(xml: Elem): Configuration = {
    @inline def bool(nme: String, default: Boolean): Boolean =
      (xml \ nme).headOption.fold(default)(_.text != "false")

    val emitInterfaces = bool("emitInterfaces", true)
    val emitClasses = bool("emitClasses", false)
    val optionToNullable = bool("optionToNullable", true)
    val optionToUndefined = bool("optionToUndefined", false)
    val prependIPrefix = bool("prependIPrefix", true)
    val prependEnclosingClassNames = bool("prependEnclosingClassNames", true)
    val typescriptIndent = (xml \ "typescriptIndent").
      headOption.fold("\t")(_.text)

    val emitCodecs = bool("emitCodecs", true)

    val fieldNaming: FieldNaming = (xml \ "fieldNaming").headOption.
      map(_.text).collect {
        case "SnakeCase" => FieldNaming.SnakeCase
      }.getOrElse(FieldNaming.Identity)

    new Configuration(
      emitInterfaces,
      emitClasses,
      optionToNullable,
      optionToUndefined,
      prependIPrefix,
      prependEnclosingClassNames,
      typescriptIndent,
      emitCodecs,
      fieldNaming)

  }
}
