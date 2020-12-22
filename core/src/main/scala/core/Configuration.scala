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
  val prependIPrefix: Boolean, // TODO: Rather interfacePrefix: Option[String]
  val prependEnclosingClassNames: Boolean,
  val typescriptIndent: String,
  val typescriptLineSeparator: Configuration.TypeScriptLineSeparator,
  val fieldNaming: FieldNaming,
  val discriminator: Configuration.Discriminator) {

  @SuppressWarnings(Array("MaxParameters"))
  private[scalats] def copy(
    emitInterfaces: Boolean = this.emitInterfaces,
    emitClasses: Boolean = this.emitClasses,
    emitCodecs: Configuration.EmitCodecs = this.emitCodecs,
    optionToNullable: Boolean = this.optionToNullable,
    optionToUndefined: Boolean = this.optionToUndefined,
    prependIPrefix: Boolean = this.prependIPrefix,
    prependEnclosingClassNames: Boolean = this.prependEnclosingClassNames,
    typescriptIndent: String = this.typescriptIndent,
    typescriptLineSeparator: Configuration.TypeScriptLineSeparator = this.typescriptLineSeparator,
    fieldNaming: FieldNaming = this.fieldNaming,
    discriminator: Configuration.Discriminator = this.discriminator): Configuration =
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
      fieldNaming,
      discriminator)

  override def equals(that: Any): Boolean = that match {
    case other: Configuration => tupled == other.tupled

    case _ => false
  }

  override def hashCode: Int = tupled.hashCode

  override def toString = tupled.toString

  private lazy val tupled = Tuple11(
    emitInterfaces,
    emitClasses,
    emitCodecs,
    optionToNullable,
    optionToUndefined,
    prependIPrefix,
    prependEnclosingClassNames,
    typescriptIndent,
    typescriptLineSeparator,
    fieldNaming,
    discriminator)
}

object Configuration {
  import io.github.scalats.tsconfig.{
    ConfigFactory,
    Config,
    ConfigException
  }

  val DefaultTypeScriptIndent = "  "

  @SuppressWarnings(Array("MaxParameters"))
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
    fieldNaming: FieldNaming = FieldNaming.Identity,
    discriminator: Discriminator = DefaultDiscriminator): Configuration =
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
      fieldNaming,
      discriminator)

  def load(
    config: Config,
    logger: Logger,
    cl: Option[ClassLoader] = None): Configuration = {

    def opt[T](key: String)(get: String => T): Option[T] = try {
      Option(get(key))
    } catch {
      case NonFatal(_: ConfigException.Missing) =>
        Option.empty[T]

      case NonFatal(cause) => {
        logger.warning(s"Fails to get $key: ${cause.getMessage}")
        Option.empty[T]
      }
    }

    @inline def bool(nme: String, default: Boolean): Boolean =
      opt(nme)(config.getBoolean(_)).getOrElse(default)

    @inline def str(nme: String): Option[String] =
      opt(nme)(config.getString(_))

    val emitInterfaces = bool("emitInterfaces", true)
    val emitClasses = bool("emitClasses", false)
    val emitCodecs = new EmitCodecs(bool("emitCodecs", true))

    val optionToNullable = bool("optionToNullable", true)
    val optionToUndefined = bool("optionToUndefined", false)
    val prependIPrefix = bool("prependIPrefix", true)
    val prependEnclosingClassNames = bool("prependEnclosingClassNames", true)
    val typescriptIndent: String =
      str("typescriptIndent").getOrElse(DefaultTypeScriptIndent)

    val typescriptLineSeparator: TypeScriptLineSeparator =
      str("typescriptLineSeparator").fold(TypeScriptSemiColon) {
        new TypeScriptLineSeparator(_)
      }

    def loadClass(n: String) =
      cl.fold[Class[_]](Class forName n)(_.loadClass(n))

    val fieldNaming: FieldNaming = str("fieldNaming").flatMap {
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

    val discriminator: Discriminator =
      str("discriminator").fold(DefaultDiscriminator) { new Discriminator(_) }

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
      fieldNaming,
      discriminator)

  }

  @SuppressWarnings(Array("NullParameter"))
  def toConfig(conf: Configuration, prefix: Option[String] = None): Config = {
    val fieldNaming: String = conf.fieldNaming match {
      case FieldNaming.SnakeCase =>
        "SnakeCase"

      case FieldNaming.Identity =>
        "Identity"

      case custom =>
        custom.getClass.getName
    }

    val repr = new java.util.HashMap[String, Any](11)
    val p = prefix.fold("") { s => s"${s}." }

    repr.put(s"${p}emitInterfaces", conf.emitInterfaces)
    repr.put(s"${p}emitClasses", conf.emitClasses)
    repr.put(s"${p}emitCodecs", conf.emitCodecs.enabled)
    repr.put(s"${p}optionToNullable", conf.optionToNullable)
    repr.put(s"${p}optionToUndefined", conf.optionToUndefined)

    repr.put(s"${p}prependIPrefix", conf.prependIPrefix)

    repr.put(
      s"${p}prependEnclosingClassNames",
      conf.prependEnclosingClassNames)

    repr.put(
      s"${p}typescriptIndent",
      conf.typescriptIndent)

    repr.put(
      s"${p}typescriptLineSeparator",
      conf.typescriptLineSeparator.value)

    repr.put(s"${p}fieldNaming", fieldNaming)

    repr.put(
      s"${p}discriminator", conf.discriminator.text)

    ConfigFactory.parseMap(repr)
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

  // ---

  final class Discriminator(val text: String) extends AnyVal {
    @inline override def toString = text
  }

  /** `"_type"` */
  val DefaultDiscriminator = new Discriminator("_type")
}
