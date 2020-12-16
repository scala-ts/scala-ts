package io.github.scalats.plugins

import java.io.File

import java.net.URL

import scala.util.control.NonFatal

import scala.xml._

import io.github.scalats.core.{
  Configuration => Settings,
  Logger,
  TypeScriptPrinter,
  TypeScriptTypeMapper
}

/**
 * @param compilationRuleSet the rule set to filter the Scala compilation units
 * @param typeRuleSet the rule set to filter the types from accepted compilation units (see [[compilationRuleSet]])
 */
final class Configuration(
  val settings: Settings,
  val compilationRuleSet: SourceRuleSet,
  val typeRuleSet: SourceRuleSet,
  val printer: TypeScriptPrinter,
  val typeScriptTypeMappers: Seq[TypeScriptTypeMapper],
  val additionalClasspath: Seq[URL]) {
  override def equals(that: Any): Boolean = that match {
    case other: Configuration => tupled == other.tupled
    case _ => false
  }

  override def hashCode: Int = tupled.hashCode

  override def toString = s"""{ compilationRuleSet: $compilationRuleSet, typeRuleSet: ${typeRuleSet}, settings${tupled.toString}, printer: ${printer}, additionalClasspath: [${additionalClasspath mkString ", "}] }"""

  private[plugins] lazy val tupled =
    Tuple5(settings, compilationRuleSet, typeRuleSet,
      printer, additionalClasspath)
}

object Configuration {
  def apply(
    settings: Settings = Settings(),
    compilationRuleSet: SourceRuleSet = SourceRuleSet(),
    typeRuleSet: SourceRuleSet = SourceRuleSet(),
    printer: TypeScriptPrinter = TypeScriptPrinter.StandardOutput,
    typeScriptTypeMappers: Seq[TypeScriptTypeMapper] = Seq(TypeScriptTypeMapper.Defaults),
    additionalClasspath: Seq[URL] = Seq.empty): Configuration =
    new Configuration(
      settings,
      compilationRuleSet, typeRuleSet,
      printer, typeScriptTypeMappers, additionalClasspath)

  /**
   * Loads the plugin configuration from given XML.
   */
  def load(
    xml: Elem,
    logger: Logger,
    printerOutputDirectory: Option[File]): Configuration = {
    val compilationRuleSet: SourceRuleSet =
      (xml \ "compilationRuleSet").headOption.fold(
        SourceRuleSet())(SourceRuleSet.load)

    val typeRuleSet: SourceRuleSet =
      (xml \ "typeRuleSet").headOption.fold(
        SourceRuleSet())(SourceRuleSet.load)

    val additionalClasspath: Seq[URL] =
      (xml \ "additionalClasspath").headOption.toSeq.
        flatMap(_ \ "url").flatMap { n =>
          try {
            Option(new URL(n.text))
          } catch {
            case NonFatal(_) =>
              logger.warning(s"Invalid URL in additional classpath: ${n.text}")
              Seq.empty[URL]
          }
        }

    val additionalClassLoader: Option[ClassLoader] = {
      if (additionalClasspath.isEmpty) None
      else {
        Option(new java.net.URLClassLoader(
          additionalClasspath.toArray,
          getClass.getClassLoader))
      }
    }

    val settings: Settings = (xml \ "settings").headOption match {
      case Some(e: Elem) =>
        Settings.load(e, logger, additionalClassLoader)

      case _ => Settings()
    }

    def customPrinter: Option[TypeScriptPrinter] =
      (xml \ "printer").headOption.map { pc =>
        val printerClass = additionalClassLoader.fold[Class[_]](
          Class.forName(pc.text))(_.loadClass(pc.text)).
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
      (xml \ "typeScriptTypeMappers" \ "class").map { tm =>
        val mapperClass = additionalClassLoader.fold[Class[_]](
          Class.forName(tm.text))(_.loadClass(tm.text)).
          asSubclass(classOf[TypeScriptTypeMapper])

        mapperClass.getDeclaredConstructor().newInstance()
      }

    new Configuration(
      settings,
      compilationRuleSet, typeRuleSet, printer,
      typeMappers, additionalClasspath)
  }

  @SuppressWarnings(Array("NullParameter"))
  def toXml(conf: Configuration, rootName: String = "scalats"): Elem = {
    def elem(n: String, children: Seq[Elem]) =
      new Elem(
        prefix = null,
        label = n,
        attributes1 = scala.xml.Null,
        scope = new scala.xml.NamespaceBinding(null, null, null),
        minimizeEmpty = true,
        children: _*)

    val children = Seq.newBuilder[Elem] ++= Seq(
      SourceRuleSet.toXml(conf.compilationRuleSet, "compilationRuleSet"),
      SourceRuleSet.toXml(conf.typeRuleSet, "typeRuleSet"),
      Settings.toXml(conf.settings, "settings"),
      elem("additionalClasspath", conf.additionalClasspath.map { url =>
        (<url>{ url }</url>)
      }))

    if (conf.printer != TypeScriptPrinter.StandardOutput) {
      children += (<printer>{ conf.printer.getClass.getName }</printer>)
    }

    if (conf.typeScriptTypeMappers.nonEmpty) {
      children += elem(
        "typeScriptTypeMappers",
        conf.typeScriptTypeMappers.map { m =>
          (<class>{ m.getClass.getName }</class>)
        })
    }

    elem(rootName, children.result())
  }
}
