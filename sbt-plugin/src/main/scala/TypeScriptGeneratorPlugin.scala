package io.github.scalats.sbt

import sbt._
import sbt.Keys._

import _root_.io.github.scalats.core.{
  Configuration => Settings,
  FieldNaming,
  TypeScriptPrinter,
  TypeScriptTypeMapper
}
import _root_.io.github.scalats.plugins.{ Configuration, FilePrinter, SourceRuleSet }

object TypeScriptGeneratorPlugin extends AutoPlugin {
  override def requires = plugins.JvmPlugin
  override def trigger = allRequirements

  // TODO: Check the documentation index.md
  object autoImport {
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

    val scalatsPrinter = settingKey[Class[_ <: TypeScriptPrinter]](
      "Class implementing 'TypeScriptPrinter' to print the generated TypeScript code according the Scala type (default: io.github.scalats.plugins.FilePrinter)")

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

    // TODO: scripted test
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

        val printer = {
          val outDir = (sourceManaged in scalatsOnCompile).value

          outDir.mkdirs()

          logger.info(s"ScalaTS printer will be initialized with directory '${outDir.getAbsolutePath}'")

          scalatsPrinter.value.
            getDeclaredConstructor(classOf[File]).newInstance(outDir)
        }

        val typeMappers = scalatsTypeScriptTypeMappers.value.map {
          _.getDeclaredConstructor().newInstance()
        }

        val conf = Configuration(
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

        val confFile = scalatsCompilerPluginConf.value.getAbsolutePath

        logger.info(s"Saving XML configuration to '${confFile}' ...")

        scala.xml.XML.save(
          filename = confFile,
          node = Configuration.toXml(conf))

        true
      } catch {
        case scala.util.control.NonFatal(cause) =>
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

        opts.result()
      }
    },
    scalatsSourceIncludes := Set(".*"),
    scalatsSourceExcludes := Set.empty[String],
    scalatsTypeIncludes := Set(".*"),
    scalatsTypeExcludes := Set.empty[String],
    scalatsPrinter := classOf[FilePrinter],
    scalatsTypeScriptTypeMappers := Seq.empty[Class[_ <: TypeScriptTypeMapper]],
    scalatsOptionToNullable := true,
    scalatsOptionToUndefined := false,
    scalatsPrependIPrefix := false,
    scalatsPrependEnclosingClassNames := false,
    scalatsTypescriptIndent := "  ",
    scalatsTypescriptLineSeparator := ";",
    scalatsEmitInterfaces := true,
    scalatsEmitClasses := false,
    scalatsEmitCodecs := true,
    scalatsFieldNaming := FieldNaming.Identity.getClass,
    scalatsDiscriminator := Settings.DefaultDiscriminator.text)
}
