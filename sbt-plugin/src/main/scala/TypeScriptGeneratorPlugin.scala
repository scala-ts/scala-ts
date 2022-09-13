package io.github.scalats.sbt

import java.io.PrintWriter

import java.net.URL

import scala.util.control.NonFatal

import scala.reflect.ClassTag

import sbt._
import sbt.Keys._
import sbt.internal.util.Attributed

import _root_.io.github.scalats.core.{
  Settings,
  TypeScriptDeclarationMapper,
  TypeScriptFieldMapper,
  TypeScriptImportResolver,
  TypeScriptPrinter,
  TypeScriptTypeMapper,
  TypeScriptTypeNaming
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

// TODO: Rename with deprecation (trait) + update doc
object TypeScriptGeneratorPlugin extends AutoPlugin {
  override def requires = plugins.JvmPlugin
  override def trigger = noTrigger

  object autoImport {

    /** Printer class, along with system properties */
    type PrinterSetting = (Class[_ <: TypeScriptPrinter], Map[String, String])

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
      settingKey[PrinterSetting]("Class implementing 'TypeScriptPrinter' to print the generated TypeScript code according the Scala type (default: io.github.scalats.plugins.FilePrinter) (with system properties to be passed in `scalacOptions`)")

    val scalatsPrinterPrelude =
      settingKey[Option[PrinterPrelude]]("Prelude for printer supporting it (e.g. `scalatsFilePrinter` or `scalatsSingleFilePrinter`); Either an in-memory string (see `scalatsPrinterInMemoryPrelude`), or a source URL (see `scalatsPrinterUrlPrelude`)")

    // TODO: Rename
    val scalatsTypeScriptImportResolvers =
      settingKey[Seq[Class[_ <: TypeScriptImportResolver]]](
        "Class implementing 'TypeScriptImportResolver' to customize the mapping (default: None)"
      )

    // TODO: Rename
    val scalatsTypeScriptDeclarationMappers =
      settingKey[Seq[Class[_ <: TypeScriptDeclarationMapper]]](
        "Class implementing 'TypeScriptDeclarationMapper' to customize the mapping (default: None)"
      )

    // TODO: Rename
    val scalatsTypeScriptTypeMappers =
      settingKey[Seq[Class[_ <: TypeScriptTypeMapper]]](
        "Class implementing 'TypeScriptTypeMapper' to customize the mapping (default: None)"
      )

    val scalatsPrependEnclosingClassNames =
      settingKey[Boolean]("Whether to prepend enclosing class/object names")

    // TODO: Rename
    val scalatsTypescriptIndent = settingKey[String](
      "Characters used as TypeScript indentation (default: 2 spaces)"
    )

    // TODO: Rename
    val scalatsTypescriptLineSeparator = settingKey[String](
      "Characters used as TypeScript line separator (default: ';')"
    )

    // TODO: Rename
    val scalatsTypeScriptTypeNaming =
      settingKey[Class[_ <: TypeScriptTypeNaming]](
        "Conversions for the field names (default: Identity)"
      )

    // TODO: Rename
    val scalatsTypeScriptFieldMapper =
      settingKey[Class[_ <: TypeScriptFieldMapper]](
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
      classOf[TypeScriptDeclarationMapper.EnumerationAsEnum]

    lazy val scalatsSingletonAsLiteral =
      classOf[TypeScriptDeclarationMapper.SingletonAsLiteral]

    lazy val scalatsValueClassAsTagged =
      classOf[TypeScriptDeclarationMapper.ValueClassAsTagged]

    lazy val scalatsUnionAsSimpleUnion =
      classOf[TypeScriptDeclarationMapper.UnionAsSimpleUnion]

    lazy val scalatsUnionWithLiteralSingletonImportResolvers =
      classOf[TypeScriptImportResolver.UnionWithLiteralSingleton]

    lazy val scalatsUnionWithLiteral: Seq[Def.Setting[_]] = Seq(
      scalatsTypeScriptDeclarationMappers ++= Seq(
        scalatsSingletonAsLiteral,
        scalatsUnionAsSimpleUnion
      ),
      scalatsTypeScriptImportResolvers ++= Seq(
        scalatsUnionWithLiteralSingletonImportResolvers
      )
    )

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
    sourceManaged in scalatsOnCompile := {
      target.value / "scala-ts" / "src_managed"
    },
    scalatsCompilerPluginConf := {
      (target in Compile).value / "scala-ts.conf"
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

        val typeNaming: TypeScriptTypeNaming = {
          val Identity = TypeScriptTypeNaming.Identity.getClass

          scalatsTypeScriptTypeNaming.value match {
            case Identity =>
              TypeScriptTypeNaming.Identity

            case cls =>
              cls.getDeclaredConstructor().newInstance()
          }
        }

        val fieldMapper: TypeScriptFieldMapper = {
          val Identity = TypeScriptFieldMapper.Identity.getClass
          val SnakeCase = TypeScriptFieldMapper.SnakeCase.getClass

          scalatsTypeScriptFieldMapper.value match {
            case Identity =>
              TypeScriptFieldMapper.Identity

            case SnakeCase =>
              TypeScriptFieldMapper.SnakeCase

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
          scalatsTypescriptIndent.value,
          new Settings.TypeScriptLineSeparator(
            scalatsTypescriptLineSeparator.value
          ),
          typeNaming,
          fieldMapper,
          new Settings.Discriminator(scalatsDiscriminator.value)
        )

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

        // Declaration mapper
        val importResolvers =
          scalatsTypeScriptImportResolvers.value.map { cls =>
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
          scalatsTypeScriptDeclarationMappers.value.map { cls =>
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
        val typeMappers = scalatsTypeScriptTypeMappers.value.map { cls =>
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
          typeScriptImportResolvers = importResolvers,
          typeScriptDeclarationMappers = declMappers,
          typeScriptTypeMappers = typeMappers,
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
    scalacOptions in Compile ++= {
      if (!scalatsOnCompile.value || !scalatsPrepare.value) {
        Seq.empty[String]
      } else {
        val opts = Seq.newBuilder[String]

        val printDir = (sourceManaged in scalatsOnCompile).value.getAbsolutePath

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
    scalatsTypeScriptImportResolvers := Seq
      .empty[Class[_ <: TypeScriptImportResolver]],
    scalatsTypeScriptDeclarationMappers := Seq
      .empty[Class[_ <: TypeScriptDeclarationMapper]],
    scalatsTypeScriptTypeMappers := Seq.empty[Class[_ <: TypeScriptTypeMapper]],
    scalatsOptionToNullable := false,
    scalatsPrependEnclosingClassNames := false,
    scalatsTypescriptIndent := "  ",
    scalatsTypescriptLineSeparator := ";",
    // scalatsEmitCodecs := false, // TODO: (medium priority)
    scalatsTypeScriptTypeNaming := TypeScriptTypeNaming.Identity.getClass,
    scalatsTypeScriptFieldMapper := TypeScriptFieldMapper.Identity.getClass,
    scalatsDiscriminator := Settings.DefaultDiscriminator.text
  )

  @SuppressWarnings(Array("NullParameter"))
  private def compilerPluginConf(
      settings: Settings,
      compilationRuleSet: SourceRuleSet,
      typeRuleSet: SourceRuleSet,
      printer: Class[_ <: TypeScriptPrinter],
      typeScriptImportResolvers: Seq[Class[_ <: TypeScriptImportResolver]],
      typeScriptDeclarationMappers: Seq[
        Class[_ <: TypeScriptDeclarationMapper]
      ],
      typeScriptTypeMappers: Seq[Class[_ <: TypeScriptTypeMapper]],
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

    if (printer != TypeScriptPrinter.StandardOutput.getClass) {
      repr.put("printer", printer.getName)
    }

    if (typeScriptImportResolvers.nonEmpty) {
      repr.put(
        "typeScriptImportResolvers",
        Arrays.asList(typeScriptImportResolvers.map(_.getName): _*)
      )

    }

    if (typeScriptDeclarationMappers.nonEmpty) {
      repr.put(
        "typeScriptDeclarationMappers",
        Arrays.asList(typeScriptDeclarationMappers.map(_.getName): _*)
      )

    }

    if (typeScriptTypeMappers.nonEmpty) {
      repr.put(
        "typeScriptTypeMappers",
        Arrays.asList(typeScriptTypeMappers.map(_.getName): _*)
      )

    }

    ConfigFactory.parseMap(repr)
  }
}
