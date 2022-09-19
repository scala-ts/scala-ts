package io.github.scalats.core

import scala.util.control.NonFatal

// TODO: Per-type options: nullable, fieldMapper, emitCodecs (by annotation on such type?)

/**
 * Created by Milosz on 09.12.2016.
 *
 * @param optionToNullable generate nullable type as `T | null`
 */
final class Settings(
    val emitCodecs: Settings.EmitCodecs,
    val optionToNullable: Boolean,
    val prependEnclosingClassNames: Boolean,
    val indent: String,
    val lineSeparator: Settings.TypeScriptLineSeparator,
    val typeNaming: TypeNaming,
    val fieldMapper: FieldMapper,
    val discriminator: Settings.Discriminator) {

  @SuppressWarnings(Array("MaxParameters", "VariableShadowing"))
  private[scalats] def copy(
      emitCodecs: Settings.EmitCodecs = this.emitCodecs,
      optionToNullable: Boolean = this.optionToNullable,
      prependEnclosingClassNames: Boolean = this.prependEnclosingClassNames,
      indent: String = this.indent,
      lineSeparator: Settings.TypeScriptLineSeparator = this.lineSeparator,
      typeNaming: TypeNaming = this.typeNaming,
      fieldMapper: FieldMapper = this.fieldMapper,
      discriminator: Settings.Discriminator = this.discriminator
    ): Settings =
    new Settings(
      emitCodecs,
      optionToNullable,
      prependEnclosingClassNames,
      indent,
      lineSeparator,
      typeNaming,
      fieldMapper,
      discriminator
    )

  override def equals(that: Any): Boolean = that match {
    case other: Settings => tupled == other.tupled

    case _ => false
  }

  override def hashCode: Int = tupled.hashCode

  override def toString = tupled.toString

  private lazy val tupled = Tuple8(
    emitCodecs,
    optionToNullable,
    prependEnclosingClassNames,
    indent,
    lineSeparator,
    typeNaming,
    fieldMapper,
    discriminator
  )
}

object Settings {
  import io.github.scalats.tsconfig.{ ConfigFactory, Config, ConfigException }

  val DefaultTypeScriptIndent = "  "

  def apply(
      emitCodecs: EmitCodecs = EmitCodecsEnabled,
      optionToNullable: Boolean = false,
      prependEnclosingClassNames: Boolean = true,
      indent: String = DefaultTypeScriptIndent,
      lineSeparator: TypeScriptLineSeparator = TypeScriptSemiColon,
      typeNaming: TypeNaming = TypeNaming.Identity,
      fieldMapper: FieldMapper = FieldMapper.Identity,
      discriminator: Discriminator = DefaultDiscriminator
    ): Settings =
    new Settings(
      emitCodecs,
      optionToNullable,
      prependEnclosingClassNames,
      indent,
      lineSeparator,
      typeNaming,
      fieldMapper,
      discriminator
    )

  def load(
      config: Config,
      logger: Logger,
      cl: Option[ClassLoader] = None
    ): Settings = {

    def opt[T](key: String)(get: String => T): Option[T] =
      try {
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

    val emitCodecs = new EmitCodecs(bool("emitCodecs", true))

    val optionToNullable = bool("optionToNullable", false)
    val prependEnclosingClassNames = bool("prependEnclosingClassNames", true)
    val indent: String =
      str("indent").getOrElse(DefaultTypeScriptIndent)

    val lineSeparator: TypeScriptLineSeparator =
      str("lineSeparator").fold(TypeScriptSemiColon) {
        new TypeScriptLineSeparator(_)
      }

    def loadClass(n: String) =
      cl.fold[Class[_]](Class forName n)(_.loadClass(n))

    val typeNaming: TypeNaming = str("typeNaming").flatMap {
      case "Identity" =>
        Some(TypeNaming.Identity)

      case className =>
        try {
          Option(
            loadClass(className)
              .asSubclass(classOf[TypeNaming])
              .getDeclaredConstructor()
              .newInstance()
          )

        } catch {
          case NonFatal(_) =>
            logger.warning(s"Fails to load custom type naming: ${className}")
            None
        }

    }.getOrElse {
      TypeNaming.Identity
    }

    val fieldMapper: FieldMapper = str("fieldMapper").flatMap {
      case "SnakeCase" =>
        Some(FieldMapper.SnakeCase)

      case "Identity" =>
        Some(FieldMapper.Identity)

      case className =>
        try {
          Option(
            loadClass(className)
              .asSubclass(classOf[FieldMapper])
              .getDeclaredConstructor()
              .newInstance()
          )

        } catch {
          case NonFatal(_) =>
            logger.warning(s"Fails to load custom field mapper: ${className}")
            None
        }

    }.getOrElse {
      FieldMapper.Identity
    }

    val discriminator: Discriminator =
      str("discriminator").fold(DefaultDiscriminator) { new Discriminator(_) }

    new Settings(
      emitCodecs,
      optionToNullable,
      prependEnclosingClassNames,
      indent,
      lineSeparator,
      typeNaming,
      fieldMapper,
      discriminator
    )

  }

  def toConfig(conf: Settings, prefix: Option[String] = None): Config = {
    val typeNaming: String = conf.typeNaming match {
      case TypeNaming.Identity =>
        "Identity"

      case custom =>
        custom.getClass.getName
    }

    val fieldMapper: String = conf.fieldMapper match {
      case FieldMapper.SnakeCase =>
        "SnakeCase"

      case FieldMapper.Identity =>
        "Identity"

      case custom =>
        custom.getClass.getName
    }

    val repr = new java.util.HashMap[String, Any](11)
    val p = prefix.fold("") { s => s"${s}." }

    repr.put(s"${p}emitCodecs", conf.emitCodecs.enabled)
    repr.put(s"${p}optionToNullable", conf.optionToNullable)

    repr.put(s"${p}prependEnclosingClassNames", conf.prependEnclosingClassNames)

    repr.put(s"${p}indent", conf.indent)

    repr.put(s"${p}lineSeparator", conf.lineSeparator.value)

    repr.put(s"${p}typeNaming", typeNaming)
    repr.put(s"${p}fieldMapper", fieldMapper)

    repr.put(s"${p}discriminator", conf.discriminator.text)

    ConfigFactory.parseMap(repr)
  }

  // ---

  final class EmitCodecs private[scalats] (
      val enabled: Boolean)
      extends AnyVal {
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
