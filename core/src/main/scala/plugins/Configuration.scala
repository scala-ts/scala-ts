package io.github.scalats.plugins

import java.io.File

import java.net.URL

import scala.util.control.NonFatal

import io.github.scalats.core.{
  Logger,
  Settings,
  TypeScriptDeclarationMapper,
  TypeScriptPrinter,
  TypeScriptTypeMapper
}

/**
 * @param settings the generator settings
 * @param compilationRuleSet the rule set to filter the Scala compilation units
 * @param typeRuleSet the rule set to filter the types from accepted compilation units (see [[compilationRuleSet]])
 * @param printer the printer to output the generated TypeScript
 */
final class Configuration(
  val settings: Settings,
  val compilationRuleSet: SourceRuleSet,
  val typeRuleSet: SourceRuleSet,
  val printer: TypeScriptPrinter,
  val typeScriptDeclarationMappers: Seq[TypeScriptDeclarationMapper],
  val typeScriptTypeMappers: Seq[TypeScriptTypeMapper],
  val additionalClasspath: Seq[URL]) {
  override def equals(that: Any): Boolean = that match {
    case other: Configuration => tupled == other.tupled
    case _ => false
  }

  override def hashCode: Int = tupled.hashCode

  override def toString = s"""{ compilationRuleSet: $compilationRuleSet, typeRuleSet: ${typeRuleSet}, settings${tupled.toString}, printer: ${printer.getClass.getName}, additionalClasspath: [${additionalClasspath mkString ", "}] }"""

  def withSettings(settings: Settings): Configuration =
    new Configuration(settings, this.compilationRuleSet, this.typeRuleSet,
      this.printer, this.typeScriptDeclarationMappers,
      this.typeScriptTypeMappers, this.additionalClasspath)

  private[plugins] lazy val tupled =
    Tuple5(settings, compilationRuleSet, typeRuleSet,
      printer, additionalClasspath)
}

@com.github.ghik.silencer.silent(".*JavaConverters.*")
object Configuration {
  import scala.collection.JavaConverters._
  import io.github.scalats.tsconfig.{ Config, ConfigException }

  def apply(
    settings: Settings = Settings(),
    compilationRuleSet: SourceRuleSet = SourceRuleSet(),
    typeRuleSet: SourceRuleSet = SourceRuleSet(),
    printer: TypeScriptPrinter = TypeScriptPrinter.StandardOutput,
    typeScriptDeclarationMappers: Seq[TypeScriptDeclarationMapper] = Seq.empty,
    typeScriptTypeMappers: Seq[TypeScriptTypeMapper] = Seq.empty, //(TypeScriptTypeMapper.Defaults),
    additionalClasspath: Seq[URL] = Seq.empty): Configuration =
    new Configuration(
      settings,
      compilationRuleSet, typeRuleSet,
      printer, typeScriptDeclarationMappers, typeScriptTypeMappers,
      additionalClasspath)

  /**
   * Loads the plugin configuration from given XML.
   */
  def load(
    config: Config,
    logger: Logger,
    printerOutputDirectory: Option[File]): Configuration = {
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

    @inline def subConf(nme: String): Option[Config] =
      opt(nme)(config.getConfig(_))

    @inline def strings(key: String): Iterable[String] = try {
      config.getStringList(key).asScala
    } catch {
      case NonFatal(_: ConfigException.Missing) =>
        List.empty[String]

      case NonFatal(cause) => {
        logger.warning(s"Fails to get $key: ${cause.getMessage}")
        List.empty[String]
      }
    }

    val compilationRuleSet: SourceRuleSet =
      subConf("compilationRuleSet").fold(SourceRuleSet())(SourceRuleSet.load)

    val typeRuleSet: SourceRuleSet =
      subConf("typeRuleSet").fold(SourceRuleSet())(SourceRuleSet.load)

    val additionalClasspath: Seq[URL] =
      strings("additionalClasspath").flatMap { n =>
        try {
          Option(new URL(n))
        } catch {
          case NonFatal(_) =>
            logger.warning(s"Invalid URL in additional classpath: ${n}")
            Seq.empty[URL]
        }
      }.toSeq

    val additionalClassLoader: Option[ClassLoader] = {
      if (additionalClasspath.isEmpty) None
      else {
        Option(new java.net.URLClassLoader(
          additionalClasspath.toArray,
          getClass.getClassLoader))
      }
    }

    val settings: Settings = subConf("settings") match {
      case Some(conf) =>
        Settings.load(conf, logger, additionalClassLoader)

      case _ => Settings()
    }

    def customPrinter: Option[TypeScriptPrinter] =
      opt("printer")(config.getString(_)).map { pc =>
        val printerClass = additionalClassLoader.fold[Class[_]](
          Class.forName(pc))(_.loadClass(pc)).
          asSubclass(classOf[TypeScriptPrinter])

        def newInstance() = printerClass.getDeclaredConstructor().newInstance()

        printerOutputDirectory match {
          case Some(dir) => try {
            printerClass.
              getDeclaredConstructor(classOf[File]).
              newInstance(dir)

          } catch {
            case NonFatal(_: NoSuchMethodException) =>
              logger.warning("Fails to initialize TypeScript printer with base directory")
              newInstance()
          }

          case _ =>
            newInstance()
        }
      }

    val printer = customPrinter.getOrElse(TypeScriptPrinter.StandardOutput)

    def typeMappers: Seq[TypeScriptTypeMapper] =
      strings("typeScriptTypeMappers").map { tm =>
        val mapperClass = additionalClassLoader.fold[Class[_]](
          Class.forName(tm))(_.loadClass(tm)).
          asSubclass(classOf[TypeScriptTypeMapper])

        mapperClass.getDeclaredConstructor().newInstance()
      }.toSeq

    def declarationMappers: Seq[TypeScriptDeclarationMapper] =
      strings("typeScriptDeclarationMappers").map { tm =>
        val mapperClass = additionalClassLoader.fold[Class[_]](
          Class.forName(tm))(_.loadClass(tm)).
          asSubclass(classOf[TypeScriptDeclarationMapper])

        mapperClass.getDeclaredConstructor().newInstance()
      }.toSeq

    new Configuration(
      settings,
      compilationRuleSet, typeRuleSet, printer,
      declarationMappers, typeMappers, additionalClasspath)
  }
}
