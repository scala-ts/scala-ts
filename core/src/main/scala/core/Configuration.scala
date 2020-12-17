package io.github.scalats.core

import scala.util.control.NonFatal

// TODO: Per-type options: nullable, fieldNaming, emitCodecs (by annotation on such type?)

/**
 * Created by Milosz on 09.12.2016.
 */
final class Configuration(
  val emitInterfaces: Boolean,
  val emitClasses: Boolean,
  val emitCodecs: Configuration.EmitCodecs,
  val optionToNullable: Boolean,
  val optionToUndefined: Boolean,
  val prependIPrefix: Boolean,
  val prependEnclosingClassNames: Boolean,
  val typescriptIndent: String,
  val typescriptLineSeparator: Configuration.TypeScriptLineSeparator,
  val fieldNaming: FieldNaming) {

  private[core] def copy(
    emitInterfaces: Boolean = this.emitInterfaces,
    emitClasses: Boolean = this.emitClasses,
    emitCodecs: Configuration.EmitCodecs = this.emitCodecs,
    optionToNullable: Boolean = this.optionToNullable,
    optionToUndefined: Boolean = this.optionToUndefined,
    prependIPrefix: Boolean = this.prependIPrefix,
    prependEnclosingClassNames: Boolean = this.prependEnclosingClassNames,
    typescriptIndent: String = this.typescriptIndent,
    typescriptLineSeparator: Configuration.TypeScriptLineSeparator = this.typescriptLineSeparator,
    fieldNaming: FieldNaming = this.fieldNaming): Configuration =
    new Configuration(
      emitInterfaces,
      emitClasses,
      emitCodecs,
      optionToNullable,
      optionToUndefined,
      prependIPrefix,
      prependEnclosingClassNames,
      typescriptIndent,
      typescriptLineSeparator,
      fieldNaming)

  override def equals(that: Any): Boolean = that match {
    case other: Configuration => tupled == other.tupled

    case _ => false
  }

  override def hashCode: Int = tupled.hashCode

  override def toString = tupled.toString

  private lazy val tupled = Tuple10(
    emitInterfaces,
    emitClasses,
    emitCodecs,
    optionToNullable,
    optionToUndefined,
    prependIPrefix,
    prependEnclosingClassNames,
    typescriptIndent,
    typescriptLineSeparator,
    fieldNaming)
}

object Configuration {
  import scala.xml._

  val DefaultTypeScriptIndent = "  "

  def apply(
    emitInterfaces: Boolean = true,
    emitClasses: Boolean = false,
    emitCodecs: EmitCodecs = EmitCodecsEnabled,
    optionToNullable: Boolean = true,
    optionToUndefined: Boolean = false,
    prependIPrefix: Boolean = true,
    prependEnclosingClassNames: Boolean = true,
    typescriptIndent: String = DefaultTypeScriptIndent,
    typescriptLineSeparator: TypeScriptLineSeparator = TypeScriptSemiColon,
    fieldNaming: FieldNaming = FieldNaming.Identity): Configuration =
    new Configuration(
      emitInterfaces,
      emitClasses,
      emitCodecs,
      optionToNullable,
      optionToUndefined,
      prependIPrefix,
      prependEnclosingClassNames,
      typescriptIndent,
      typescriptLineSeparator,
      fieldNaming)

  def load(
    xml: Elem,
    logger: Logger,
    cl: Option[ClassLoader] = None): Configuration = {
    @inline def bool(nme: String, default: Boolean): Boolean =
      (xml \ nme).headOption.fold(default)(_.text != "false")

    val emitInterfaces = bool("emitInterfaces", true)
    val emitClasses = bool("emitClasses", false)
    val emitCodecs = bool("emitCodecs", true)

    val optionToNullable = bool("optionToNullable", true)
    val optionToUndefined = bool("optionToUndefined", false)
    val prependIPrefix = bool("prependIPrefix", true)
    val prependEnclosingClassNames = bool("prependEnclosingClassNames", true)
    val typescriptIndent = (xml \ "typescriptIndent").
      headOption.fold(DefaultTypeScriptIndent)(_.text)

    val typescriptLineSeparator = (xml \ "typescriptLineSeparator").
      headOption.fold(TypeScriptSemiColon) { n =>
        new TypeScriptLineSeparator(n.text)
      }

    def loadClass(n: String) =
      cl.fold[Class[_]](Class forName n)(_.loadClass(n))

    val fieldNaming: FieldNaming =
      (xml \ "fieldNaming").headOption.map(_.text).flatMap {
        case "SnakeCase" =>
          Some(FieldNaming.SnakeCase)

        case "Identity" =>
          Some(FieldNaming.Identity)

        case className =>
          try {
            Option(loadClass(className).
              asSubclass(classOf[FieldNaming]).
              getDeclaredConstructor().newInstance())

          } catch {
            case NonFatal(_) =>
              logger.warning(s"Fails to load custom field naming: ${className}")
              None
          }

      }.getOrElse {
        FieldNaming.Identity
      }

    new Configuration(
      emitInterfaces,
      emitClasses,
      new EmitCodecs(emitCodecs),
      optionToNullable,
      optionToUndefined,
      prependIPrefix,
      prependEnclosingClassNames,
      typescriptIndent,
      typescriptLineSeparator,
      fieldNaming)

  }

  @SuppressWarnings(Array("NullParameter"))
  def toXml(conf: Configuration, rootName: String = "scalats"): Elem = {
    val fieldNaming: String = conf.fieldNaming match {
      case FieldNaming.SnakeCase =>
        "SnakeCase"

      case FieldNaming.Identity =>
        "Identity"

      case custom =>
        custom.getClass.getName
    }

    new Elem(
      prefix = null,
      label = rootName,
      attributes1 = scala.xml.Null,
      scope = new scala.xml.NamespaceBinding(null, null, null),
      minimizeEmpty = true,
      <emitInterfaces>{ conf.emitInterfaces }</emitInterfaces>,
      <emitClasses>{ conf.emitClasses }</emitClasses>,
      <emitCodecs>{ conf.emitCodecs.enabled }</emitCodecs>,
      <optionToNullable>{ conf.optionToNullable }</optionToNullable>,
      <optionToUndefined>{ conf.optionToUndefined }</optionToUndefined>,
      <prependIPrefix>{ conf.prependIPrefix }</prependIPrefix>,
      <prependEnclosingClassNames>{ conf.prependEnclosingClassNames }</prependEnclosingClassNames>,
      <typescriptIndent>{ conf.typescriptIndent }</typescriptIndent>,
      <typescriptLineSeparator>{ conf.typescriptLineSeparator }</typescriptLineSeparator>,
      <fieldNaming>{ fieldNaming }</fieldNaming>)
  }

  // ---

  final class EmitCodecs private[scalats] (
    val enabled: Boolean) extends AnyVal {
    @inline override def toString = enabled.toString
  }

  val EmitCodecsEnabled = new EmitCodecs(true)
  val EmitCodecsDisabled = new EmitCodecs(true)

  // ---

  final class TypeScriptLineSeparator(val value: String) extends AnyVal {
    @inline override def toString = value
  }

  val TypeScriptSemiColon = new TypeScriptLineSeparator(";")
}
