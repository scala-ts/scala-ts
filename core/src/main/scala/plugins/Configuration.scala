package io.github.scalats.plugins

import java.io.File

import java.net.{ URI, URL }

import scala.util.control.NonFatal

import scala.reflect.ClassTag

import io.github.scalats.core.{
  DeclarationMapper,
  ImportResolver,
  Logger,
  Printer,
  Settings,
  TypeMapper
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
    val printer: Printer,
    val importResolvers: Seq[ImportResolver],
    val declarationMappers: Seq[DeclarationMapper],
    val typeMappers: Seq[TypeMapper],
    val additionalClasspath: Seq[URL]) {

  override def equals(that: Any): Boolean = that match {
    case other: Configuration => tupled == other.tupled
    case _                    => false
  }

  override def hashCode: Int = tupled.hashCode

  override def toString =
    s"""{ compilationRuleSet: $compilationRuleSet, typeRuleSet: ${typeRuleSet}, settings${tupled.toString}, printer: ${printer.getClass.getName}, additionalClasspath: [${additionalClasspath mkString ", "}] }"""

  def withSettings(_settings: Settings): Configuration =
    new Configuration(
      _settings,
      this.compilationRuleSet,
      this.typeRuleSet,
      this.printer,
      this.importResolvers,
      this.declarationMappers,
      this.typeMappers,
      this.additionalClasspath
    )

  private[plugins] lazy val tupled =
    Tuple5(
      settings,
      compilationRuleSet,
      typeRuleSet,
      printer,
      additionalClasspath
    )
}

@com.github.ghik.silencer.silent(".*JavaConverters.*")
object Configuration {
  import scala.collection.JavaConverters._
  import io.github.scalats.tsconfig.{ Config, ConfigException }

  def apply(
      settings: Settings = Settings(),
      compilationRuleSet: SourceRuleSet = SourceRuleSet(),
      typeRuleSet: SourceRuleSet = SourceRuleSet(),
      printer: Printer = Printer.StandardOutput,
      importResolvers: Seq[ImportResolver] = Seq.empty,
      declarationMappers: Seq[DeclarationMapper] = Seq.empty,
      typeMappers: Seq[TypeMapper] = Seq.empty, // (TypeMapper.Defaults),
      additionalClasspath: Seq[URL] = Seq.empty
    ): Configuration =
    new Configuration(
      settings,
      compilationRuleSet,
      typeRuleSet,
      printer,
      importResolvers,
      declarationMappers,
      typeMappers,
      additionalClasspath
    )

  /**
   * Loads the plugin configuration from given XML.
   */
  def load(
      config: Config,
      logger: Logger,
      printerOutputDirectory: Option[File]
    ): Configuration = {
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

    val strs = strings(logger, config, _: String)

    @inline def subConf(nme: String): Option[Config] =
      opt(nme)(config.getConfig(_))

    val compilationRuleSet: SourceRuleSet =
      subConf("compilationRuleSet").fold(SourceRuleSet())(SourceRuleSet.load)

    val typeRuleSet: SourceRuleSet =
      subConf("typeRuleSet").fold(SourceRuleSet())(SourceRuleSet.load)

    val additionalClasspath: Seq[URL] =
      strs("additionalClasspath").flatMap { n =>
        try {
          Option(new URI(n).toURL)
        } catch {
          case NonFatal(_) =>
            logger.warning(s"Invalid URL in additional classpath: ${n}")
            Seq.empty[URL]
        }
      }.toSeq

    val additionalClassLoader: Option[ClassLoader] = {
      if (additionalClasspath.isEmpty) None
      else {
        Option(
          new java.net.URLClassLoader(
            additionalClasspath.toArray,
            getClass.getClassLoader
          )
        )
      }
    }

    val settings: Settings = subConf("settings") match {
      case Some(conf) =>
        Settings.load(conf, logger, additionalClassLoader)

      case _ => Settings()
    }

    @SuppressWarnings(Array("AsInstanceOf"))
    def customPrinter: Option[Printer] =
      opt("printer")(config.getString(_)).map { pc =>
        val printerClass: Class[Printer] = additionalClassLoader
          .fold[Class[_]](Class.forName(pc))(_.loadClass(pc))
          .asInstanceOf[Class[Printer]]

        def newInstance() =
          printerClass.getDeclaredConstructor().newInstance()

        printerOutputDirectory match {
          case Some(dir) =>
            try {
              printerClass
                .getDeclaredConstructor(classOf[File])
                .newInstance(dir)

            } catch {
              case NonFatal(_: NoSuchMethodException) =>
                logger.warning(
                  "Fails to initialize TypeScript printer with base directory"
                )
                newInstance()
            }

          case _ =>
            newInstance()
        }
      }

    val printer = customPrinter.getOrElse(Printer.StandardOutput)

    def insts[T: ClassTag](key: String): Seq[T] =
      instances[T](logger, config, additionalClassLoader, key)

    def typeMappers: Seq[TypeMapper] = insts[TypeMapper]("typeMappers")

    def importResolvers: Seq[ImportResolver] =
      insts[ImportResolver]("importResolvers")

    def declarationMappers: Seq[DeclarationMapper] =
      insts[DeclarationMapper]("declarationMappers")

    new Configuration(
      settings,
      compilationRuleSet,
      typeRuleSet,
      printer,
      importResolvers,
      declarationMappers,
      typeMappers,
      additionalClasspath
    )
  }

  @inline private def strings(
      logger: Logger,
      config: Config,
      key: String
    ): Iterable[String] =
    try {
      config.getStringList(key).asScala
    } catch {
      case NonFatal(_: ConfigException.Missing) =>
        List.empty[String]

      case NonFatal(cause) => {
        logger.warning(s"Fails to get $key: ${cause.getMessage}")
        List.empty[String]
      }
    }

  @inline private def instances[T](
      logger: Logger,
      config: Config,
      classLoader: Option[ClassLoader],
      key: String
    )(implicit
      ct: ClassTag[T]
    ): Seq[T] =
    strings(logger, config, key).flatMap { nme =>
      val cls = classLoader
        .fold[Class[_]](Class.forName(nme))(_.loadClass(nme))
        .asSubclass(ct.runtimeClass)

      cls.getDeclaredConstructor().newInstance() match {
        case `ct`(instance) =>
          Seq[T](instance)

        case _ => Seq.empty[T]
      }
    }.toSeq

}
