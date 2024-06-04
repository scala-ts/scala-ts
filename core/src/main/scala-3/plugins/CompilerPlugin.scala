package io.github.scalats.plugins

import java.io.File

import dotty.tools.dotc.report
import dotty.tools.dotc.core.Flags
import dotty.tools.dotc.core.Symbols.Symbol
import dotty.tools.dotc.core.Types.Type
import dotty.tools.dotc.core.Contexts.Context
import dotty.tools.dotc.ast.tpd
import dotty.tools.dotc.plugins.{ PluginPhase, StandardPlugin }
import dotty.tools.dotc.transform.{ Pickler, Staging }

import scala.util.matching.Regex

import scala.collection.immutable.ListSet

import io.github.scalats.core.{
  Logger,
  ScalaParser,
  DeclarationMapper,
  Generator,
  ImportResolver,
  TypeMapper
}
import io.github.scalats.tsconfig.ConfigFactory

final class CompilerPlugin extends StandardPlugin:
  val name = ScalaTSPhase.name
  val description = "Scala compiler plugin for TypeScript (scalats)"

  def init(options: List[String]): List[PluginPhase] =
    (new ScalaTSPhase(ScalaTSPhase initer options)) :: Nil

  override val optionsHelp: Option[String] =
    Some("  -P:scalats:configuration=/path/to/config             Path to the plugin configuration as XML file\r\n  -P:scalats:debug             Enable plugin debug")

end CompilerPlugin

private[plugins] object ScalaTSPhase:
  val name = "scalats"

  type Initer = Context => (Configuration, Boolean)

  def initer(opts: List[String]): Initer = {
    val cfgPrefix = "configuration="
    val outDirPrefix = "printerOutputDirectory="

    val logger = new DeferredLogger

    @annotation.tailrec
    def parse(
        options: List[String],
        outDir: File,
        cfgPath: Option[String],
        debug: Boolean
      ): (Configuration, Boolean) = options.headOption match {
      case Some(opt) => {
        if (opt startsWith cfgPrefix) {
          parse(options.tail, outDir, Some(opt stripPrefix cfgPrefix), debug)
        } else if (opt startsWith outDirPrefix) {
          val dir = new File(opt stripPrefix outDirPrefix)

          logger.info(s"{${ScalaTSPhase.name}} Set printer output directory: ${dir.getAbsolutePath}")

          parse(options.tail, dir, cfgPath, debug)
        } else if (opt startsWith "sys.") {
          // Set system property related to scala-ts passed as plugin options

          val prop = opt.stripPrefix("sys.")

          prop.span(_ != '=') match {
            case (_, "") =>
              logger.info(
                s"{${ScalaTSPhase.name}} Ignore invalid option: ${opt}"
              )

            case (key, rv) =>
              sys.props.put(key, rv.stripPrefix("="))
          }

          parse(options.tail, outDir, cfgPath, debug)
        } else if (opt == "debug" || opt == "debug=true") {
          parse(options.tail, outDir, cfgPath, true)
        } else {
          parse(options.tail, outDir, cfgPath, debug)
        }
      }

      case _ => {
        val config: Configuration = cfgPath match {
          case Some(configPath) => {
            val c = Configuration.load(
              ConfigFactory.parseFile(new File(configPath)),
              logger,
              Option(outDir)
            )

            logger.info(s"{${ScalaTSPhase.name}} Configuration loaded from '${configPath}': $c")

            c
          }

          case _ => {
            logger.info(
              s"{${ScalaTSPhase.name}} Loading the default configuration"
            )

            Configuration()
          }
        }

        config -> debug
      }
    }

    val inited = parse(opts, new File("."), None, false)

    import tpd.*

    { (ctx: Context) =>
      given context: Context = ctx

      // Apply the deferred logger
      logger(new ReportLogger(report))

      inited
    }
  }

  private[plugins] class ReportLogger(
      r: report.type
    )(using
      Context)
      extends Logger:
    def debug(msg: => String): Unit = r.inform(msg)
    def info(msg: => String): Unit = r.echo(msg)
    def warning(msg: => String): Unit = r.warning(msg)

end ScalaTSPhase

private class ScalaTSPhase(initer: ScalaTSPhase.Initer) extends PluginPhase {
  override val runsAfter = Set(Pickler.name)
  override val runsBefore = Set(Staging.name)

  val phaseName = ScalaTSPhase.name

  import tpd.*

  override def run(
      using
      ctx: Context
    ): Unit = {
    val compiledFile: File = ctx.source.file.file
    val (config, debug) = initer(ctx)

    import config.compilationRuleSet

    val acceptsCompilationUnit: File => Boolean = {
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

    if (acceptsCompilationUnit(compiledFile)) {
      if (debug) {
        report.inform(
          s"${phaseName}.debug: Checking ${compiledFile.getAbsolutePath}"
        )
      }

      handle(config, debug)
    } else if (debug) {
      report.inform(
        s"${phaseName}.debug: Skip excluded ${compiledFile.getAbsolutePath}"
      )
    }
  }

  // ---

  @annotation.tailrec
  private def matches(str: String, s: Set[Regex]): Boolean =
    s.headOption match {
      case Some(re) =>
        re.findFirstIn(str) match {
          case Some(_) => true

          case _ => matches(str, s.tail)
        }

      case _ => false
    }

  @volatile private var examined = ListSet.empty[ScalaParser.TypeFullId]
  private val compiled = scala.collection.mutable.Set.empty[String]

  private def handle(
      config: Configuration,
      debug: Boolean
    )(using
      ctx: Context
    ): Unit = {
    import config.typeRuleSet

    val acceptsType: Symbol => Boolean = {
      val includes = typeRuleSet.includes.map(_.r)
      val excludes = typeRuleSet.excludes.map(_.r)

      { sym =>
        val fullName = {
          val isModule = sym.is(Flags.Module) || sym.is(Flags.ModuleClass)

          if (isModule) s"object:${sym.fullName}"
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
    def traverse[Acc](
        trees: Seq[Tree],
        acc: Acc,
        handled: List[Type] = List.empty
      )(f: ((Type, Tree), Acc) => Acc
      ): Acc =
      trees.headOption match {
        case Some(tree) => {
          import tree.{ tpe, symbol => sym }

          val isModule = sym.is(Flags.Module) || sym.is(Flags.ModuleClass)
          val isPackage = sym.is(Flags.Package) || sym.is(Flags.PackageClass)
          val isTypeDef = tree.isInstanceOf[TypeDef]

          if (
            !(sym.fullName.startsWith("java.") ||
              sym.fullName.startsWith("scala.")) &&
            ((isModule && isTypeDef && !isPackage) || (sym.isClass && !isModule))
          ) {
            lazy val kind: String = if (isModule) "object" else "class"

            val b = Seq.newBuilder[Tree]

            tree.filterSubTrees(_ != tree).foreach { b += _ }

            val newTrees: Seq[Tree] = b.result() ++: trees.tail

            val accepted = (isTypeDef || tree.isType) && acceptsType(sym)
            val add = !handled.contains(tpe) && accepted

            val newAcc = {
              if (add) {
                if (debug) {
                  report.inform(
                    s"${phaseName}.debug: Handling $kind ${sym.fullName}"
                  )
                }

                f(tpe -> tree, acc)
              } else {
                if (!accepted && debug) {
                  report.inform(
                    s"${phaseName}.debug: Skip excluded '$kind:${sym.fullName}'"
                  )
                }

                acc
              }
            }

            val newEx = {
              if (add) tpe :: handled
              else handled
            }

            traverse(newTrees, newAcc, newEx)(f)
          } else {
            traverse(trees.tail, acc, handled)(f)
          }
        }

        case _ =>
          acc
      }

    val typeBuf = List.newBuilder[(Type, Tree)]

    def body: List[Tree] = {
      val children = List.newBuilder[Tree]

      ctx.compilationUnit.tpdTree.foreachSubTree {
        children += _
      }

      children.result()
    }

    val symtab =
      (traverse(body, List.empty[(String, (Type, Tree))]) {
        case (ref @ (_, tree), acc) =>
          typeBuf += ref
          tree.symbol.fullName.toString -> ref :: acc
      }).toMap

    val scalaTypes = typeBuf.result()

    val declMapper = DeclarationMapper
      .chain(config.declarationMappers)
      .getOrElse(DeclarationMapper.Defaults)

    val typeMapper =
      TypeMapper.chain(config.typeMappers).getOrElse(TypeMapper.Defaults)

    val importResolver = ImportResolver
      .chain(config.importResolvers)
      .getOrElse(ImportResolver.Defaults)

    compiled.synchronized {
      // Include the current compilation unit as it's known
      compiled += ctx.source.file.canonicalPath
    }

    val ex = Generator.generate(
      settings = config.settings,
      types = scalaTypes,
      symtab = symtab,
      logger = new ScalaTSPhase.ReportLogger(report),
      importResolver = importResolver,
      declMapper = declMapper,
      typeMapper = typeMapper,
      printer = config.printer,
      examined = examined,
      compiled = compiled.toSet,
      acceptsType = acceptsType
    )

    examined = examined ++ ex
  }
}
