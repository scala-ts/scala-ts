package io.github.scalats.plugins

import java.io.File

import scala.tools.nsc.{ Global, Phase }
import scala.tools.nsc.plugins.{ Plugin, PluginComponent }

import scala.util.matching.Regex

import scala.collection.immutable.ListSet

import io.github.scalats.core.{
  Logger,
  ScalaParser,
  TypeScriptDeclarationMapper,
  TypeScriptGenerator,
  TypeScriptImportResolver,
  TypeScriptTypeMapper
}
import io.github.scalats.tsconfig.ConfigFactory

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

      if (opt startsWith "sys.") {
        // Set system property related to scala-ts passed as plugin options

        val prop = opt.stripPrefix("sys.")

        prop.span(_ != '=') match {
          case (_, "") =>
            global.inform(s"{${plugin.name}} Ignore invalid option: ${opt}")

          case (key, rv) =>
            sys.props.put(key, rv.stripPrefix("="))
        }
      }

      if (opt == "debug" || opt == "debug=true") {
        debug = true
      }
    }

    if (configPath != null) {
      config = Configuration.load(
        ConfigFactory.parseFile(new File(configPath)),
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

    @volatile private var examined = ListSet.empty[ScalaParser.TypeFullId]

    private def handle(
      unit: CompilationUnit,
      acceptsType: Symbol => Boolean): Unit = {

      @annotation.tailrec
      def traverse[Acc](
        trees: Seq[Tree],
        acc: Acc,
        examined: List[Type] = List.empty)(
        f: ((Type, Tree), Acc) => Acc): Acc =
        trees.headOption match {
          case Some(tree) => {
            import tree.{ symbol => sym }

            if (sym != null && !(sym.fullName.startsWith("java.") ||
              sym.fullName.startsWith("scala.")) &&
              ((sym.isModule && !sym.hasPackageFlag) || sym.isClass)) {

              val tpe = sym.typeSignature
              lazy val kind: String = if (sym.isModule) "object" else "class"

              val b = Seq.newBuilder[Tree]

              tree.filter(_ != tree).foreach { b += _ }

              val newTrees: Seq[Tree] = { // TODO: Check
                //if (!sym.isModule) trees.tail
                //else tree.children ++: trees.tail

                b.result() ++: trees.tail
              }

              val accepted = (
                ClassDefTag.unapply(tree).nonEmpty ||
                ModuleDefTag.unapply(tree).nonEmpty) && acceptsType(sym)

              val add = !examined.contains(tpe) && accepted

              val newAcc = {
                if (add) {
                  if (plugin.debug) {
                    global.inform(s"${plugin.name}.debug: Handling $kind ${sym.fullName}")
                  }

                  f(tpe -> tree, acc)
                } else {
                  if (!accepted && plugin.debug) {
                    global.inform(s"${plugin.name}.debug: Skip excluded '$kind:${sym.fullName}'")
                  }

                  acc
                }
              }

              val newEx = {
                if (add) tpe :: examined
                else examined
              }

              traverse(newTrees, newAcc, newEx)(f)
            } else {
              traverse(trees.tail, acc, examined)(f)
            }
          }

          case _ =>
            acc
        }

      val symtab = (traverse(
        unit.body.children, List.empty[(String, (Type, Tree))]) {
          case ((tpe, tree), acc) =>
            tree.symbol.fullName -> (tpe -> tree) :: acc
        }).toMap

      val scalaTypes: List[(Type, Tree)] = (traverse(
        unit.body.children, List.empty[(Type, Tree)]) { _ :: _ }).reverse

      object CompilerLogger extends io.github.scalats.core.Logger {
        def info(msg: => String): Unit = plugin.global.inform(msg)
        def warning(msg: => String): Unit = plugin.warning(msg)
      }

      val declMapper = TypeScriptDeclarationMapper.
        chain(config.typeScriptDeclarationMappers).
        getOrElse(TypeScriptDeclarationMapper.Defaults)

      val typeMapper = TypeScriptTypeMapper.
        chain(config.typeScriptTypeMappers).
        getOrElse(TypeScriptTypeMapper.Defaults)

      val importResolver = TypeScriptImportResolver.
        chain(config.typeScriptImportResolvers).
        getOrElse(TypeScriptImportResolver.Defaults)

      val ex = TypeScriptGenerator.generate(global)(
        settings = plugin.config.settings,
        types = scalaTypes,
        symtab = symtab,
        logger = CompilerLogger,
        importResolver = importResolver,
        declMapper = declMapper,
        typeMapper = typeMapper,
        printer = config.printer,
        examined = examined)

      examined = examined ++ ex
    }
  }
}
