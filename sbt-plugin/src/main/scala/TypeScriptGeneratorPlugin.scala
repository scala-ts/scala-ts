package io.github.scalats.sbt

import java.net.URL

import scala.util.control.NonFatal

import scala.reflect.ClassTag

import sbt._
import sbt.Keys._

import _root_.io.github.scalats.core.{
  Configuration => Settings,
  FieldNaming,
  TypeScriptPrinter,
  TypeScriptTypeMapper
}
import _root_.io.github.scalats.plugins.{ FilePrinter, SingleFilePrinter, SourceRuleSet }

object TypeScriptGeneratorPlugin extends AutoPlugin {
  override def requires = plugins.JvmPlugin
  override def trigger = allRequirements

  // TODO: Check the documentation index.md
  object autoImport {
    /** Printer class, along with system properties */
    type PrinterSetting = (Class[_ <: TypeScriptPrinter], Map[String, String])

    /** Printer preload, either as in-memory lines, or from a source URL */
    type PrinterPrelude = Either[Seq[String], URL]

    val scalatsOnCompile = settingKey[Boolean](
      "Enable ScalaTS generation on compilation (default: true)")

    val scalatsPrepare = taskKey[Boolean]("Prepare ScalaTS")

    val scalatsDebug = settingKey[Boolean](
      "Enable ScalaTS debug (default: false)")

    val scalatsCompilerPluginConf = settingKey[File](
      "Path to the configuration file generated for the compiler plugin")

    val scalatsEmitInterfaces = settingKey[Boolean](
      "Generate interface declarations")

    val scalatsEmitClasses = settingKey[Boolean]("Generate class declarations")

    val scalatsEmitCodecs = settingKey[Boolean](
      "EXPERIMENTAL: Generate the codec functions fromData/toData for TypeScript classes")

    val scalatsOptionToNullable = settingKey[Boolean](
      "Option types will be compiled to 'type | null'")

    val scalatsOptionToUndefined = settingKey[Boolean](
      "Option types will be compiled to 'type | undefined'")

    val scalatsPrinter =
      settingKey[PrinterSetting](
        "Class implementing 'TypeScriptPrinter' to print the generated TypeScript code according the Scala type (default: io.github.scalats.plugins.FilePrinter) (with system properties to be passed in `scalacOptions`)")

    val scalatsPrinterPrelude =
      settingKey[Option[PrinterPrelude]]("Prelude for printer supporting it (e.g. `scalatsFilePrinter` or `scalatsSingleFilePrinter`); Either an in-memory string, or a source URL")

    val scalatsTypeScriptTypeMappers = settingKey[Seq[Class[_ <: TypeScriptTypeMapper]]]("Class implementing 'TypeScriptTypeMapper' to customize the mapping (default: None)")

    val scalatsPrependIPrefix = settingKey[Boolean](
      "Whether to prefix interface names with 'I'")

    val scalatsPrependEnclosingClassNames = settingKey[Boolean](
      "Whether to prepend enclosing class/object names")

    val scalatsTypescriptIndent = settingKey[String](
      "Characters used as TypeScript indentation (default: 2 spaces)")

    val scalatsTypescriptLineSeparator = settingKey[String](
      "Characters used as TypeScript line separator (default: ';')")

    val scalatsFieldNaming = settingKey[Class[_ <: FieldNaming]]("Conversions for the field names if 'scalatsEmitCodecs' (default: Identity)")

    // TODO: (medium priority) scripted test
    val scalatsDiscriminator = settingKey[String](
      "Name for the discriminator field")

    val scalatsSourceIncludes = settingKey[Set[String]](
      "Scala sources to be included for ScalaTS (default: '.*'")

    val scalatsSourceExcludes = settingKey[Set[String]](
      "Scala sources to be excluded for ScalaTS (default: none)")

    val scalatsTypeIncludes = settingKey[Set[String]](
      "Scala types to be included for ScalaTS (default '.*'")

    val scalatsTypeExcludes = settingKey[Set[String]](
      "Scala types to be excluded for ScalaTS (default: none)")

    // ---

    lazy val scalatsNullableAsOption =
      classOf[TypeScriptTypeMapper.NullableAsOption]

    lazy val scalatsDateAsString =
      classOf[TypeScriptTypeMapper.DateAsString]

    lazy val scalatsNumberAsString =
      classOf[TypeScriptTypeMapper.NumberAsString]

    // ---

    @SuppressWarnings(Array("AsInstanceOf"))
    def scalatsPrinterForClass[C <: TypeScriptPrinter](
      props: (String, String)*)(implicit ct: ClassTag[C]): PrinterSetting =
      ct.runtimeClass.asInstanceOf[Class[C]] -> Map(props: _*)

    /** Print one file per type */
    lazy val scalatsFilePrinter: PrinterSetting =
      classOf[FilePrinter] -> Map.empty[String, String]

    // TODO: scripted test
    lazy val scalatsSingleFilePrinter: PrinterSetting =
      classOf[SingleFilePrinter] -> Map.empty[String, String]

    // TODO: scripted test
    def scalatsSingleFilePrinter(filename: String): PrinterSetting =
      classOf[SingleFilePrinter] -> Map("scala-ts.single-filename" -> filename)

    @inline def scalatsPrinterInMemoryPrelude(
      content: String*): Option[PrinterPrelude] =
      Some(Left(content))

    @inline def scalatsPrinterUrlPrelude(source: URL): Option[PrinterPrelude] =
      Some(Right(source))
  }

  import autoImport._
  import Manifest._

  override lazy val projectSettings: Seq[Def.Setting[_]] = Seq(
    scalatsOnCompile := true,
    scalatsDebug := false,
    autoCompilerPlugins := true,
    addCompilerPlugin(groupId %% coreArtifactId % version),
    sourceManaged in scalatsOnCompile := {
      target.value / "scala-ts" / "src_managed"
    },
    scalatsCompilerPluginConf := (target in Compile).value / "plugin-conf.xml",
    scalatsPrepare := {
      val logger = streams.value.log
      import Settings.EmitCodecs

      try {
        val sbtScalaVer: String = {
          val props = new java.util.Properties
          props.load(getClass getResourceAsStream "/library.properties")

          val Major = "^([0-9]+\\.[0-9]+).*$".r

          Option(props getProperty "version.number").collect {
            case Major(v) => v
          }.getOrElse {
            logger.warn("Fails to resolve SBT scala version; Defaults to 2.12")
            "2.12"
          }
        }

        val sbtMajorVer: String = {
          if (sbtBinaryVersion.value == "0.13") "0.13"
          else "1.0"
        }

        val sbtProjectClassUrl = {
          val t = baseDirectory.value / "project" / "target" / s"scala-${sbtScalaVer}" / s"sbt-${sbtMajorVer}" / "classes"

          t.toURI.toURL
        }

        val fieldNaming: FieldNaming = {
          val Identity = FieldNaming.Identity.getClass
          val SnakeCase = FieldNaming.SnakeCase.getClass

          scalatsFieldNaming.value match {
            case Identity =>
              FieldNaming.Identity

            case SnakeCase =>
              FieldNaming.SnakeCase

            case cls =>
              cls.getDeclaredConstructor().newInstance()
          }
        }

        if ((scalatsEmitClasses.value && scalatsEmitInterfaces.value) &&
          !scalatsPrependIPrefix.value) {

          logger.warn(s"Both 'scalatsEmitClasses' and 'scalatsEmitInterfaces' are enabled, without `scalatsPrependIPrefix := true` this can lead to invalid generation")
        }

        // Overall settings
        val settings = Settings(
          scalatsEmitInterfaces.value,
          scalatsEmitClasses.value,
          new EmitCodecs(scalatsEmitCodecs.value),
          scalatsOptionToNullable.value,
          scalatsOptionToUndefined.value,
          scalatsPrependIPrefix.value,
          scalatsPrependEnclosingClassNames.value,
          scalatsTypescriptIndent.value,
          new Settings.TypeScriptLineSeparator(
            scalatsTypescriptLineSeparator.value),
          fieldNaming,
          new Settings.Discriminator(scalatsDiscriminator.value))

        // Printer
        val printer = {
          val outDir = (sourceManaged in scalatsOnCompile).value

          outDir.mkdirs()

          logger.info(s"ScalaTS printer will be initialized with directory '${outDir.getAbsolutePath}'")

          val cls = scalatsPrinter.value._1

          try {
            cls.getDeclaredConstructor(classOf[File])
          } catch {
            case NonFatal(_) =>
              logger.error(s"Invalid printer class: ${cls.getName}")
          }

          cls
        }

        scalatsPrinterPrelude.value match {
          case Some(Left(content)) =>
            io.IO.writeLines(target.value / "scala-ts-prelude.tmp", content)

          case _ =>
            ()
        }

        // Type mapper
        val typeMappers = scalatsTypeScriptTypeMappers.value.map { cls =>
          try {
            cls.getDeclaredConstructor()
          } catch {
            case NonFatal(_) =>
              logger.error(
                s"Invalid TypeScript type mapper class: ${cls.getName}")
          }

          cls
        }

        val confFile = scalatsCompilerPluginConf.value.getAbsolutePath

        logger.info(s"Saving XML configuration to '${confFile}' ...")

        val conf = xmlConfiguration(
          settings = settings,
          compilationRuleSet = SourceRuleSet(
            includes = scalatsSourceIncludes.value,
            excludes = scalatsSourceExcludes.value),
          typeRuleSet = SourceRuleSet(
            includes = scalatsSourceIncludes.value,
            excludes = scalatsSourceExcludes.value),
          printer = printer,
          typeScriptTypeMappers = typeMappers,
          additionalClasspath = Seq(sbtProjectClassUrl))

        scala.xml.XML.save(filename = confFile, node = conf)

        true
      } catch {
        case NonFatal(cause) =>
          logger.error("Fails to prepare ScalaTS execution")
          cause.printStackTrace()

          false
      }
    },
    scalacOptions in Compile ++= {
      if (!scalatsOnCompile.value || !scalatsPrepare.value) {
        Seq.empty[String]
      } else {
        val opts = Seq.newBuilder[String]

        val printDir = (sourceManaged in scalatsOnCompile).value.getAbsolutePath

        opts ++= Seq(
          s"-P:scalats:configuration=${scalatsCompilerPluginConf.value.getAbsolutePath}",
          s"-P:scalats:printerOutputDirectory=${printDir}")

        if (scalatsDebug.value) {
          opts += "-P:scalats:debug"
        }

        scalatsPrinter.value._2.foreach {
          case (key, value) =>
            opts += s"-P:scalats:sys.${key}=${value}"
        }

        scalatsPrinterPrelude.value match {
          case Some(Right(url)) =>
            opts += s"-P:scalats:sys.scala-ts.printer.prelude-url=${url.toString}"

          case Some(_) => {
            val f = target.value / "scala-ts-prelude.tmp"
            // `f` will be written with content in `scalatsPrepare`

            opts += s"-P:scalats:sys.scala-ts.printer.prelude-url=${f.toURI.toString}"
          }

          case _ =>
        }

        opts.result()
      }
    },
    scalatsSourceIncludes := Set(".*"),
    scalatsSourceExcludes := Set.empty[String],
    scalatsTypeIncludes := Set(".*"),
    scalatsTypeExcludes := Set.empty[String],
    scalatsPrinter := scalatsFilePrinter,
    scalatsPrinterPrelude := Option.empty[PrinterPrelude],
    scalatsTypeScriptTypeMappers := Seq.empty[Class[_ <: TypeScriptTypeMapper]],
    scalatsOptionToNullable := true,
    scalatsOptionToUndefined := false,
    scalatsPrependIPrefix := false,
    scalatsPrependEnclosingClassNames := false,
    scalatsTypescriptIndent := "  ",
    scalatsTypescriptLineSeparator := ";",
    scalatsEmitInterfaces := true,
    scalatsEmitClasses := false,
    scalatsEmitCodecs := false,
    scalatsFieldNaming := FieldNaming.Identity.getClass,
    scalatsDiscriminator := Settings.DefaultDiscriminator.text)

  import scala.xml.Elem

  @SuppressWarnings(Array("NullParameter"))
  private def xmlConfiguration(
    settings: Settings,
    compilationRuleSet: SourceRuleSet,
    typeRuleSet: SourceRuleSet,
    printer: Class[_ <: TypeScriptPrinter],
    typeScriptTypeMappers: Seq[Class[_ <: TypeScriptTypeMapper]],
    additionalClasspath: Seq[URL]): Elem = {
    val rootName = "scalats"

    def elem(n: String, children: Seq[Elem]) =
      new Elem(
        prefix = null,
        label = n,
        attributes1 = scala.xml.Null,
        scope = new scala.xml.NamespaceBinding(null, null, null),
        minimizeEmpty = true,
        children: _*)

    val children = Seq.newBuilder[Elem] ++= Seq(
      SourceRuleSet.toXml(compilationRuleSet, "compilationRuleSet"),
      SourceRuleSet.toXml(typeRuleSet, "typeRuleSet"),
      Settings.toXml(settings, "settings"),
      elem("additionalClasspath", additionalClasspath.map { url =>
        (<url>{ url }</url>)
      }))

    if (printer != TypeScriptPrinter.StandardOutput.getClass) {
      children += (<printer>{ printer.getName }</printer>)
    }

    if (typeScriptTypeMappers.nonEmpty) {
      children += elem(
        "typeScriptTypeMappers",
        typeScriptTypeMappers.map { cls =>
          (<class>{ cls.getName }</class>)
        })
    }

    elem(rootName, children.result())
  }
}
