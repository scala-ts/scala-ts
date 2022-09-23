package io.github.scalats.plugins

import java.io.PrintStream

import io.github.scalats.ast.TypeRef
import io.github.scalats.core.{ Printer, Settings }
import io.github.scalats.core.Internals.ListSet

abstract class BasePrinter extends Printer {
  private lazy val preludeUrl = sys.props.get("scala-ts.printer.prelude-url")

  /**
   * If the system property `scala-ts.printer.prelude-url` is defined,
   * then print the content from the URL as prelude to the given stream.
   */
  protected def printPrelude(out: PrintStream): Unit =
    preludeUrl.foreach { url =>
      out.println(scala.io.Source.fromURL(url).mkString)
    }

  private lazy val preformatImport: (String, String) => String =
    sys.props.get("scala-ts.printer.import-pattern") match {
      case Some(pattern) =>
        pattern.format(_: String, _: String)

      case _ => { (tpeName: String, importPath: String) =>
        s"import * as ns${tpeName} from '${importPath}';\nimport type { ${tpeName} } from '${importPath}'"
      }
    }

  /**
   * Prints TypeScript imports from the required types.
   * If the system property `scala-ts.printer.import-pattern`
   *
   * @param importPath the function applied to each required type to determine the import path
   */
  protected def printImports(
      settings: Settings,
      requires: ListSet[TypeRef],
      out: PrintStream
    )(importPath: TypeRef => String
    ): Unit = {
    import settings.{ lineSeparator => sep }

    val typeNaming = settings.typeNaming(settings, _: TypeRef)
    val requiredTypes = requires.toList.sortBy(_.name)

    requiredTypes.foreach { tpe =>
      val tpeName = typeNaming(tpe)
      val preformatted = preformatImport(tpeName, importPath(tpe))

      out.println(s"${preformatted}${sep}")
    }
  }
}
