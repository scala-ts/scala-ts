package org.scalats.plugins

import java.io.File

import scala.xml.XML

import scala.util.matching.Regex

//import scala.reflect.internal.util.Position
import scala.tools.nsc.plugins.{ Plugin, PluginComponent }
import scala.tools.nsc.{ Global, Phase }

import org.scalats.core.TypeScriptGenerator

final class CompilerPlugin(val global: Global) extends Plugin { plugin =>
  val name = "scalats"
  val description = "Scala compiler plugin for TypeScript (scalats)"
  val components: List[PluginComponent] = List(Component)

  private var config: Configuration = _
  private var debug: Boolean = false

  override def processOptions(options: List[String], error: String => Unit): Unit = {
    val prefix = "configuration="

    options.foreach { opt =>
      if (opt startsWith prefix) {
        config = Configuration.load(XML.loadFile(opt stripPrefix prefix))

        global.inform(s"${plugin.name}: Loaded configuration: $config")
      }

      if (opt == "debug" || opt == "debug=true") {
        debug = true
      }
    }

    if (config == null) {
      config = Configuration()

      global.inform(s"${plugin.name}: Defaulting configuration")
    }
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

      val scalaTypes: List[Type] = unit.body.children.flatMap { x =>
        val sym = x.symbol

        if (sym.isModule && !sym.hasPackageFlag) {
          if (acceptsType(sym)) {
            if (plugin.debug) {
              global.inform(s"${plugin.name}.debug: Handling object ${sym.fullName}")
            }

            Seq(sym.typeSignature)
          } else {
            if (plugin.debug) {
              global.inform(s"${plugin.name}.debug: Skip excluded 'object:${sym.fullName}'")
            }

            Seq.empty
          }
        } else if (sym.isClass) {
          if (acceptsType(sym)) {
            if (plugin.debug) {
              global.inform(s"${plugin.name}.debug: Handling class ${sym.fullName}")
            }

            Seq(sym.typeSignature)
          } else {
            if (plugin.debug) {
              global.inform(s"${plugin.name}.debug: Skip excluded 'class:${sym.fullName}'")
            }

            Seq.empty
          }
        } else {
          Seq.empty
        }
      }

      object CompilerLogger extends org.scalats.core.Logger {
        def warning(msg: => String): Unit = global.warning(msg)
      }

      TypeScriptGenerator.generate(global)(
        config = plugin.config.settings,
        types = scalaTypes,
        logger = CompilerLogger,
        out = { _ => Console.out /* TODO: as parameter/from conf? */ })

    }
  }
}
