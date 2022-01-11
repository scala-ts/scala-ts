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
    val typescriptIndent: String,
    val typescriptLineSeparator: Settings.TypeScriptLineSeparator,
    val typeNaming: TypeScriptTypeNaming,
    val fieldMapper: TypeScriptFieldMapper,
    val discriminator: Settings.Discriminator) {

  @SuppressWarnings(Array("MaxParameters"))
  private[scalats] def copy(
      emitCodecs: Settings.EmitCodecs = this.emitCodecs,
      optionToNullable: Boolean = this.optionToNullable,
      prependEnclosingClassNames: Boolean = this.prependEnclosingClassNames,
      typescriptIndent: String = this.typescriptIndent,
      typescriptLineSeparator: Settings.TypeScriptLineSeparator =
        this.typescriptLineSeparator,
      typeNaming: TypeScriptTypeNaming = this.typeNaming,
      fieldMapper: TypeScriptFieldMapper = this.fieldMapper,
      discriminator: Settings.Discriminator = this.discriminator
    ): Settings =
    new Settings(
      emitCodecs,
      optionToNullable,
      prependEnclosingClassNames,
      typescriptIndent,
      typescriptLineSeparator,
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
    typescriptIndent,
    typescriptLineSeparator,
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
      typescriptIndent: String = DefaultTypeScriptIndent,
      typescriptLineSeparator: TypeScriptLineSeparator = TypeScriptSemiColon,
      typeNaming: TypeScriptTypeNaming = TypeScriptTypeNaming.Identity,
      fieldMapper: TypeScriptFieldMapper = TypeScriptFieldMapper.Identity,
      discriminator: Discriminator = DefaultDiscriminator
    ): Settings =
    new Settings(
      emitCodecs,
      optionToNullable,
      prependEnclosingClassNames,
      typescriptIndent,
      typescriptLineSeparator,
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
    val typescriptIndent: String =
      str("typescriptIndent").getOrElse(DefaultTypeScriptIndent)

    val typescriptLineSeparator: TypeScriptLineSeparator =
      str("typescriptLineSeparator").fold(TypeScriptSemiColon) {
        new TypeScriptLineSeparator(_)
      }

    def loadClass(n: String) =
      cl.fold[Class[_]](Class forName n)(_.loadClass(n))

    val typeNaming: TypeScriptTypeNaming = str("typeNaming").flatMap {
      case "Identity" =>
        Some(TypeScriptTypeNaming.Identity)

      case className =>
        try {
          Option(
            loadClass(className)
              .asSubclass(classOf[TypeScriptTypeNaming])
              .getDeclaredConstructor()
              .newInstance()
          )

        } catch {
          case NonFatal(_) =>
            logger.warning(s"Fails to load custom type naming: ${className}")
            None
        }

    }.getOrElse {
      TypeScriptTypeNaming.Identity
    }

    val fieldMapper: TypeScriptFieldMapper = str("fieldMapper").flatMap {
      case "SnakeCase" =>
        Some(TypeScriptFieldMapper.SnakeCase)

      case "Identity" =>
        Some(TypeScriptFieldMapper.Identity)

      case className =>
        try {
          Option(
            loadClass(className)
              .asSubclass(classOf[TypeScriptFieldMapper])
              .getDeclaredConstructor()
              .newInstance()
          )

        } catch {
          case NonFatal(_) =>
            logger.warning(s"Fails to load custom field mapper: ${className}")
            None
        }

    }.getOrElse {
      TypeScriptFieldMapper.Identity
    }

    val discriminator: Discriminator =
      str("discriminator").fold(DefaultDiscriminator) { new Discriminator(_) }

    new Settings(
      emitCodecs,
      optionToNullable,
      prependEnclosingClassNames,
      typescriptIndent,
      typescriptLineSeparator,
      typeNaming,
      fieldMapper,
      discriminator
    )

  }

  def toConfig(conf: Settings, prefix: Option[String] = None): Config = {
    val typeNaming: String = conf.typeNaming match {
      case TypeScriptTypeNaming.Identity =>
        "Identity"

      case custom =>
        custom.getClass.getName
    }

    val fieldMapper: String = conf.fieldMapper match {
      case TypeScriptFieldMapper.SnakeCase =>
        "SnakeCase"

      case TypeScriptFieldMapper.Identity =>
        "Identity"

      case custom =>
        custom.getClass.getName
    }

    val repr = new java.util.HashMap[String, Any](11)
    val p = prefix.fold("") { s => s"${s}." }

    repr.put(s"${p}emitCodecs", conf.emitCodecs.enabled)
    repr.put(s"${p}optionToNullable", conf.optionToNullable)

    repr.put(s"${p}prependEnclosingClassNames", conf.prependEnclosingClassNames)

    repr.put(s"${p}typescriptIndent", conf.typescriptIndent)

    repr.put(s"${p}typescriptLineSeparator", conf.typescriptLineSeparator.value)

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
