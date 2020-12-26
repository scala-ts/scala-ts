package io.github.scalats.plugins

import java.io.PrintStream

import io.github.scalats.core.{ Settings, TypeScriptPrinter }
import io.github.scalats.typescript.TypeRef

abstract class BasePrinter extends TypeScriptPrinter {
  /**
   * If the system property `scala-ts.printer.prelude-url` is defined,
   * then print the content from the URL as prelude to the given stream.
   */
  protected def printPrelude(out: PrintStream): Unit =
    sys.props.get("scala-ts.printer.prelude-url").foreach { url =>
      out.println(scala.io.Source.fromURL(url).mkString)
    }

  /**
   * Prints TypeScript imports from the required types.
   *
   * @param importPath the function applied to each required type to determine the import path
   */
  protected def printImports(
    settings: Settings,
    requires: Set[TypeRef],
    out: PrintStream)(importPath: TypeRef => String): Unit = {
    import settings.{ typescriptLineSeparator => sep }

    val typeNaming = settings.typeNaming(settings, _: TypeRef)

    requires.foreach { tpe =>
      val tpeName = typeNaming(tpe)

      out.println(s"import { ${tpeName} } from '${importPath(tpe)}'${sep}")
    }

    if (requires.nonEmpty) {
      out.println()
    }
  }
}
