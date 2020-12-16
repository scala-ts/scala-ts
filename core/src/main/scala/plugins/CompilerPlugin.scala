package io.github.scalats.plugins

import java.io.File

import scala.tools.nsc.{ Global, Phase }
import scala.tools.nsc.plugins.{ Plugin, PluginComponent }

import scala.util.matching.Regex

import scala.xml.XML

import io.github.scalats.core.{
  Logger,
  TypeScriptTypeMapper,
  TypeScriptGenerator
}

final class CompilerPlugin(val global: Global)
  extends Plugin with PluginCompat { plugin =>

  val name = "scalats"
  val description = "Scala compiler plugin for TypeScript (scalats)"
  val components: List[PluginComponent] = List(Component)

  private var config: Configuration = _
  private var printerOutputDirectory: File = _
  private var debug: Boolean = false

  @SuppressWarnings(Array("NullParameter", "NullAssignment"))
  override def init(
    options: List[String],
    error: String => Unit): Boolean = {
    val cfgPrefix = "configuration="
    val outDirPrefix = "printerOutputDirectory="

    printerOutputDirectory = new File(".")

    var configPath: String = null

    options.foreach { opt =>
      if (opt startsWith cfgPrefix) {
        configPath = opt.stripPrefix(cfgPrefix)
      }

      if (opt startsWith outDirPrefix) {
        printerOutputDirectory = new File(opt stripPrefix outDirPrefix)

        global.inform(s"{${plugin.name}} Set printer output directory: ${printerOutputDirectory.getAbsolutePath}")
      }

      if (opt == "debug" || opt == "debug=true") {
        debug = true
      }
    }

    if (configPath != null) {
      config = Configuration.load(
        XML.loadFile(configPath),
        Logger(global),
        Option(printerOutputDirectory))

      global.inform(s"{${plugin.name}} Configuration loaded from '${configPath}': $config")
    }

    if (config == null) {
      config = Configuration()

      global.inform(s"{${plugin.name}} Loading the default configuration")
    }

    true
  }

  override val optionsHelp: Option[String] = Some(
    "  -P:scalats:configuration=/path/to/config             Path to the plugin configuration as XML file\r\n  -P:scalats:debug             Enable plugin debug")

  private object Component extends PluginComponent {
    val global: plugin.global.type = plugin.global
    val runsAfter = List("typer")
    val phaseName = plugin.name

    import global._

    def newPhase(prev: Phase): StdPhase = new StdPhase(prev) {
      val config = plugin.config

      import config.{ compilationRuleSet, typeRuleSet }

      @inline override def name = plugin.name

      def apply(unit: CompilationUnit): Unit = {
        val compiledFile = unit.source.file.file

        if (acceptsCompilationUnit(compiledFile)) {
          if (plugin.debug) {
            global.inform(s"${plugin.name}.debug: Checking ${compiledFile.getAbsolutePath}")
          }

          handle(unit, acceptsType)
        } else if (plugin.debug) {
          global.inform(s"${plugin.name}.debug: Skip excluded ${compiledFile.getAbsolutePath}")
        }
      }

      // ---

      lazy val acceptsCompilationUnit: File => Boolean = {
        val includes = compilationRuleSet.includes.map(_.r)
        val excludes = compilationRuleSet.excludes.map(_.r)

        { unit =>
          val path = unit.getAbsolutePath

          if (!matches(path, includes)) {
            false
          } else {
            !matches(path, excludes)
          }
        }
      }

      lazy val acceptsType: Symbol => Boolean = {
        val includes = typeRuleSet.includes.map(_.r)
        val excludes = typeRuleSet.excludes.map(_.r)

        { sym =>
          val fullName = {
            if (sym.isModule) s"object:${sym.fullName}"
            else s"class:${sym.fullName}"
          }

          if (!matches(fullName, includes)) {
            false
          } else {
            !matches(fullName, excludes)
          }
        }
      }

      @annotation.tailrec
      private def matches(str: String, s: Set[Regex]): Boolean =
        s.headOption match {
          case Some(re) => re.findFirstIn(str) match {
            case Some(_) => true

            case _ => matches(str, s.tail)
          }

          case _ => false
        }

    }

    private def handle(
      unit: CompilationUnit,
      acceptsType: Symbol => Boolean): Unit = {

      @annotation.tailrec
      def go(syms: Seq[Symbol], tpes: List[Type]): List[Type] =
        syms.headOption match {
          case Some(sym) => {
            if ((sym.isModule && !sym.hasPackageFlag) || sym.isClass) {
              val tpe = sym.typeSignature
              lazy val kind: String = if (sym.isModule) "object" else "class"

              if (acceptsType(sym)) {
                if (plugin.debug) {
                  global.inform(s"${plugin.name}.debug: Handling $kind ${sym.fullName}")
                }

                if (sym.isModule) {
                  go(tpe.members ++: syms.tail, tpe :: tpes)
                } else {
                  go(syms.tail, tpe :: tpes)
                }
              } else {
                if (plugin.debug) {
                  global.inform(s"${plugin.name}.debug: Skip excluded '$kind:${sym.fullName}'")
                }

                if (sym.isModule) {
                  go(tpe.members ++: syms.tail, tpes)
                } else {
                  go(syms.tail, tpes)
                }
              }
            } else {
              go(syms.tail, tpes)
            }
          }

          case _ =>
            tpes.reverse
        }

      val scalaTypes: List[Type] = go(unit.body.children.map(_.symbol), Nil)

      object CompilerLogger extends io.github.scalats.core.Logger {
        def warning(msg: => String): Unit = plugin.warning(msg)
      }

      val typeMapper = TypeScriptTypeMapper.
        chain(config.typeScriptTypeMappers).
        getOrElse(TypeScriptTypeMapper.Defaults)

      TypeScriptGenerator.generate(global)(
        config = plugin.config.settings,
        types = scalaTypes,
        logger = CompilerLogger,
        out = config.printer,
        typeMapper = typeMapper)
    }
  }
}
