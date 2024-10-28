package io.github.scalats.sbt

import java.io.PrintWriter

import java.net.URL

import scala.util.control.NonFatal

import scala.reflect.ClassTag

import sbt._
import sbt.Keys._
import sbt.internal.util.Attributed

import _root_.io.github.scalats.core.{
  DeclarationMapper,
  FieldMapper,
  ImportResolver,
  Printer,
  Settings,
  TypeMapper,
  TypeNaming
}
import _root_.io.github.scalats.plugins.{
  FilePrinter,
  SingleFilePrinter,
  SourceRuleSet
}
import _root_.io.github.scalats.tsconfig.{
  Config,
  ConfigFactory,
  ConfigRenderOptions
}

object ScalatsGeneratorPlugin extends AutoPlugin {
  override def requires = plugins.JvmPlugin
  override def trigger = noTrigger

  object autoImport {
    import _root_.io.github.scalats.core.Printer

    /** Printer class, along with system properties */
    type PrinterSetting = (Class[_ <: Printer], Map[String, String])

    /** Printer preload, either as in-memory lines, or from a source URL */
    type PrinterPrelude = Either[Seq[String], URL]

    val scalatsOnCompile = settingKey[Boolean](
      "Enable ScalaTS generation on compilation (default: true)"
    )

    val scalatsPrepare = taskKey[Boolean]("Prepare ScalaTS")

    val scalatsDebug =
      settingKey[Boolean]("Enable ScalaTS debug (default: false)")

    val scalatsCompilerPluginConf = settingKey[File](
      "Path to the configuration file generated for the compiler plugin"
    )

    val scalatsOptionToNullable =
      settingKey[Boolean]("Option types will be compiled to 'type | null'")

    val scalatsPrinter =
      settingKey[PrinterSetting]("Class implementing 'Printer' to print the generated TypeScript code according the Scala type (default: io.github.scalats.plugins.FilePrinter) (with system properties to be passed in `scalacOptions`)")

    val scalatsPrinterPrelude =
      settingKey[Option[PrinterPrelude]]("Prelude for printer supporting it (e.g. `scalatsFilePrinter` or `scalatsSingleFilePrinter`); Either an in-memory string (see `scalatsPrinterInMemoryPrelude`), or a source URL (see `scalatsPrinterUrlPrelude`)")

    @deprecated("Use scalatsImportResolvers", "0.5.14")
    @inline def scalatsTypeScriptImportResolvers = scalatsImportResolvers

    val scalatsImportResolvers =
      settingKey[Seq[Class[_ <: ImportResolver]]](
        "Class implementing 'ImportResolver' to customize the mapping (default: None)"
      )

    @deprecated("Use scalatsDeclarationMappers", "0.5.14")
    @inline def scalatsTypeScriptDeclarationMappers = scalatsDeclarationMappers

    val scalatsDeclarationMappers =
      settingKey[Seq[Class[_ <: DeclarationMapper]]](
        "Class implementing 'DeclarationMapper' to customize the mapping (default: None)"
      )

    @deprecated("Use scalatsTypeMappers", "0.5.14")
    @inline def scalatsTypeScriptTypeMappers = scalatsTypeMappers

    val scalatsTypeMappers =
      settingKey[Seq[Class[_ <: TypeMapper]]](
        "Class implementing 'TypeMapper' to customize the mapping (default: None)"
      )

    val scalatsPrependEnclosingClassNames =
      settingKey[Boolean]("Whether to prepend enclosing class/object names")

    @deprecated("Use scalatsIndent", "0.5.14")
    @inline def scalatsTypescriptIndent = scalatsIndent

    val scalatsIndent = settingKey[String](
      "Characters used as TypeScript indentation (default: 2 spaces)"
    )

    @deprecated("Use scalatsLineSeparator", "0.5.14")
    @inline def scalatsTypescriptLineSeparator = scalatsLineSeparator

    val scalatsLineSeparator = settingKey[String](
      "Characters used as TypeScript line separator (default: ';')"
    )

    @deprecated("Use scalatsTypeNaming", "0.5.14")
    @inline def scalatsTypeScriptTypeNaming = scalatsTypeNaming

    val scalatsTypeNaming =
      settingKey[Class[_ <: TypeNaming]](
        "Conversions for the field names (default: Identity)"
      )

    @deprecated("Use scalatsFieldMapper", "0.5.14")
    @inline def scalatsTypeScriptFieldMapper = scalatsFieldMapper

    val scalatsFieldMapper =
      settingKey[Class[_ <: FieldMapper]](
        "Conversions for the field names (default: Identity)"
      )

    // TODO: (medium priority) scripted test
    val scalatsDiscriminator =
      settingKey[String]("Name for the discriminator field")

    val scalatsSourceIncludes = settingKey[Set[String]]( // TODO: Regex
      "Scala sources to be included for ScalaTS (default: '.*'"
    )

    val scalatsSourceExcludes = settingKey[Set[String]](
      "Scala sources to be excluded for ScalaTS (default: none)"
    )

    private val typeRegex =
      "Regular expressions on type full names; Can be prefixed with either 'object:' or 'class:' (for class or trait)"

    val scalatsTypeIncludes = settingKey[Set[String]](
      s"Scala types to be included for ScalaTS; $typeRegex (default '.*'"
    )

    val scalatsTypeExcludes = settingKey[Set[String]](
      s"Scala types to be excluded for ScalaTS; $typeRegex (default: none)"
    )

    val scalatsAdditionalClasspath =
      taskKey[Classpath]("Additional classpath for Scala-TS")

    // ---

    lazy val scalatsEnumerationAsEnum =
      classOf[DeclarationMapper.EnumerationAsEnum]

    lazy val scalatsSingletonAsLiteral =
      classOf[DeclarationMapper.SingletonAsLiteral]

    lazy val scalatsValueClassAsTagged =
      classOf[DeclarationMapper.ValueClassAsTagged]

    lazy val scalatsUnionAsSimpleUnion =
      classOf[DeclarationMapper.UnionAsSimpleUnion]

    lazy val scalatsUnionWithLiteralSingletonImportResolvers =
      classOf[ImportResolver.UnionWithLiteralSingleton]

    lazy val scalatsUnionWithLiteral: Seq[Def.Setting[_]] = Seq(
      scalatsDeclarationMappers ++= Seq(
        scalatsSingletonAsLiteral,
        scalatsUnionAsSimpleUnion
      ),
      scalatsImportResolvers ++= Seq(
        scalatsUnionWithLiteralSingletonImportResolvers
      )
    )

    // ---

    lazy val scalatsNullableAsOption =
      classOf[TypeMapper.NullableAsOption]

    lazy val scalatsDateAsString =
      classOf[TypeMapper.DateAsString]

    lazy val scalatsNumberAsString =
      classOf[TypeMapper.NumberAsString]

    // ---

    @SuppressWarnings(Array("AsInstanceOf"))
    def scalatsPrinterForClass[C <: Printer](
        props: (String, String)*
      )(implicit
        ct: ClassTag[C]
      ): PrinterSetting =
      ct.runtimeClass.asInstanceOf[Class[C]] -> Map(props: _*)

    /** Print one file per type */
    lazy val scalatsFilePrinter: PrinterSetting =
      classOf[FilePrinter] -> Map.empty[String, String]

    lazy val scalatsSingleFilePrinter: PrinterSetting =
      classOf[SingleFilePrinter] -> Map.empty[String, String]

    def scalatsSingleFilePrinter(filename: String): PrinterSetting =
      classOf[SingleFilePrinter] -> Map("scala-ts.single-filename" -> filename)

    @inline def scalatsPrinterInMemoryPrelude(
        content: String*
      ): Option[PrinterPrelude] =
      Some(Left(content))

    @inline def scalatsPrinterUrlPrelude(source: URL): Option[PrinterPrelude] =
      Some(Right(source))

    private val moduleIdKey = AttributeKey[ModuleID]("moduleID")

    def scalatsAddScalatsDependency(
        dependency: ModuleID
      ): Seq[Def.Setting[_]] = {
      val m = compilerPlugin(dependency)

      Seq(
        libraryDependencies += m,
        scalatsAdditionalClasspath ++= {
          val log = streams.value.log
          val v = scalaBinaryVersion.value

          val jars = Classpaths.managedJars(
            sbt.librarymanagement.Configurations.CompilerPlugin,
            Set("jar"),
            update.value
          )

          jars.find(jm =>
            jm.get(moduleIdKey).exists { ji =>
              ji.organization == m.organization && (ji.name == m.name || ji.name
                .startsWith(s"${m.name}_${v}"))
            }
          ) match {
            case Some(jar) => {
              log.debug(s"Resolve library dependency '${name}': ${jar.data}")

              Seq(jar)
            }

            case _ => {
              log.error(s"Fails to resolve library dependency: $name")

              Seq.empty
            }
          }
        }
      )
    }
  }

  import autoImport._
  import Manifest._

  override lazy val projectSettings: Seq[Def.Setting[_]] = Seq(
    scalatsOnCompile := true,
    scalatsDebug := false,
    autoCompilerPlugins := true,
    addCompilerPlugin(groupId %% coreArtifactId % version),
    scalatsOnCompile / sourceManaged := {
      target.value / "scala-ts" / "src_managed"
    },
    scalatsCompilerPluginConf := {
      (Compile / target).value / "scala-ts.conf"
    },
    scalatsAdditionalClasspath := {
      val sbtScalaVer: String = {
        val props = new java.util.Properties
        props.load(getClass getResourceAsStream "/library.properties")

        val Major = "^([0-9]+\\.[0-9]+).*$".r

        Option(props getProperty "version.number").collect {
          case Major(v) => v
        }.getOrElse {
          println("Fails to resolve SBT scala version; Defaults to 2.12")

          "2.12"
        }
      }

      val sbtMajorVer: String = {
        if (sbtBinaryVersion.value == "0.13") "0.13"
        else "1.0"
      }

      Seq(
        Attributed.blank(
          baseDirectory.value / "project" / "target" / s"scala-${sbtScalaVer}" / s"sbt-${sbtMajorVer}" / "classes"
        )
      )
    },
    scalatsPrepare := {
      val logger = streams.value.log
      import Settings.EmitCodecs

      var out: PrintWriter = null

      try {
        val additionalClasspath = scalatsAdditionalClasspath.value.map {
          _.data.toURI.toURL
        }

        val typeNaming: TypeNaming = {
          val Identity = TypeNaming.Identity.getClass

          scalatsTypeNaming.value match {
            case Identity =>
              TypeNaming.Identity

            case cls =>
              cls.getDeclaredConstructor().newInstance()
          }
        }

        val fieldMapper: FieldMapper = {
          val Identity = FieldMapper.Identity.getClass
          val SnakeCase = FieldMapper.SnakeCase.getClass

          scalatsFieldMapper.value match {
            case Identity =>
              FieldMapper.Identity

            case SnakeCase =>
              FieldMapper.SnakeCase

            case cls =>
              cls.getDeclaredConstructor().newInstance()
          }
        }

        // Overall settings
        val settings = Settings(
          new EmitCodecs(
            false
          ), // TODO: (medium priority) scalatsEmitCodecs.value
          scalatsOptionToNullable.value,
          scalatsPrependEnclosingClassNames.value,
          scalatsIndent.value,
          new Settings.TypeScriptLineSeparator(
            scalatsLineSeparator.value
          ),
          typeNaming,
          fieldMapper,
          new Settings.Discriminator(scalatsDiscriminator.value)
        )

        // Printer
        val printer = {
          val outDir = (scalatsOnCompile / sourceManaged).value

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

        // Declaration mapper
        val importResolvers =
          scalatsImportResolvers.value.map { cls =>
            try {
              cls.getDeclaredConstructor()
            } catch {
              case NonFatal(_) =>
                logger.error(
                  s"Invalid TypeScript import resolver class: ${cls.getName}"
                )
            }

            cls
          }

        // Declaration mapper
        val declMappers =
          scalatsDeclarationMappers.value.map { cls =>
            try {
              cls.getDeclaredConstructor()
            } catch {
              case NonFatal(_) =>
                logger.error(
                  s"Invalid TypeScript type mapper class: ${cls.getName}"
                )
            }

            cls
          }

        // Type mapper
        val typeMappers = scalatsTypeMappers.value.map { cls =>
          try {
            cls.getDeclaredConstructor()
          } catch {
            case NonFatal(_) =>
              logger.error(
                s"Invalid TypeScript type mapper class: ${cls.getName}"
              )
          }

          cls
        }

        val confFile = scalatsCompilerPluginConf.value.getAbsolutePath

        logger.info(
          s"Saving compiler plugin configuration to '${confFile}' ..."
        )

        val conf = compilerPluginConf(
          settings = settings,
          compilationRuleSet = SourceRuleSet(
            includes = scalatsSourceIncludes.value,
            excludes = scalatsSourceExcludes.value
          ),
          typeRuleSet = SourceRuleSet(
            includes = scalatsTypeIncludes.value,
            excludes = scalatsTypeExcludes.value
          ),
          printer = printer,
          importResolvers = importResolvers,
          declarationMappers = declMappers,
          typeMappers = typeMappers,
          additionalClasspath = additionalClasspath
        )

        out = new PrintWriter(confFile)

        out.print(conf.root.render(ConfigRenderOptions.concise))
        out.flush()

        true
      } catch {
        case NonFatal(cause) =>
          logger.error("Fails to prepare ScalaTS execution")
          cause.printStackTrace()

          false
      } finally {
        if (out != null) {
          try {
            out.close()
          } catch {
            case NonFatal(_) =>
          }
        }
      }
    },
    Compile / scalacOptions ++= {
      if (!scalatsOnCompile.value || !scalatsPrepare.value) {
        Seq.empty[String]
      } else {
        val opts = Seq.newBuilder[String]

        val printDir = (scalatsOnCompile / sourceManaged).value.getAbsolutePath

        opts ++= Seq(
          s"-P:scalats:configuration=${scalatsCompilerPluginConf.value.getAbsolutePath}",
          s"-P:scalats:printerOutputDirectory=${printDir}"
        )

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
    scalatsPrinterPrelude := scalatsPrinterInMemoryPrelude(
      s"// Generated by ScalaTS ${version}: https://scala-ts.github.io/scala-ts/"
    ),
    scalatsImportResolvers := Seq.empty[Class[_ <: ImportResolver]],
    scalatsDeclarationMappers := Seq.empty[Class[_ <: DeclarationMapper]],
    scalatsTypeMappers := Seq.empty[Class[_ <: TypeMapper]],
    scalatsOptionToNullable := false,
    scalatsPrependEnclosingClassNames := false,
    scalatsIndent := "  ",
    scalatsLineSeparator := ";",
    // scalatsEmitCodecs := false, // TODO: (medium priority)
    scalatsTypeNaming := TypeNaming.Identity.getClass,
    scalatsFieldMapper := FieldMapper.Identity.getClass,
    scalatsDiscriminator := Settings.DefaultDiscriminator.text
  )

  @SuppressWarnings(Array("NullParameter"))
  private def compilerPluginConf(
      settings: Settings,
      compilationRuleSet: SourceRuleSet,
      typeRuleSet: SourceRuleSet,
      printer: Class[_ <: Printer],
      importResolvers: Seq[Class[_ <: ImportResolver]],
      declarationMappers: Seq[Class[_ <: DeclarationMapper]],
      typeMappers: Seq[Class[_ <: TypeMapper]],
      additionalClasspath: Seq[URL]
    ): Config = {

    import java.util.Arrays

    val repr = new java.util.HashMap[String, Any](6)

    repr.put(
      "compilationRuleSet",
      SourceRuleSet.toConfig(compilationRuleSet).root
    )

    repr.put("typeRuleSet", SourceRuleSet.toConfig(typeRuleSet).root)
    repr.put("settings", Settings.toConfig(settings).root)

    repr.put(
      "additionalClasspath",
      Arrays.asList(additionalClasspath.map(_.toString): _*)
    )

    if (printer != Printer.StandardOutput.getClass) {
      repr.put("printer", printer.getName)
    }

    if (importResolvers.nonEmpty) {
      repr.put(
        "importResolvers",
        Arrays.asList(importResolvers.map(_.getName): _*)
      )

    }

    if (declarationMappers.nonEmpty) {
      repr.put(
        "declarationMappers",
        Arrays.asList(declarationMappers.map(_.getName): _*)
      )

    }

    if (typeMappers.nonEmpty) {
      repr.put(
        "typeMappers",
        Arrays.asList(typeMappers.map(_.getName): _*)
      )

    }

    ConfigFactory.parseMap(repr)
  }
}
