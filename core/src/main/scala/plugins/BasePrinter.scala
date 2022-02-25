package io.github.scalats.plugins

import java.io.PrintStream

import io.github.scalats.core.{ Settings, TypeScriptPrinter }
import io.github.scalats.core.Internals.ListSet
import io.github.scalats.typescript.TypeRef

abstract class BasePrinter extends TypeScriptPrinter {
  private lazy val preludeUrl = sys.props.get("scala-ts.printer.prelude-url")

  /**
   * If the system property `scala-ts.printer.prelude-url` is defined,
   * then print the content from the URL as prelude to the given stream.
   */
  protected def printPrelude(out: PrintStream): Unit =
    preludeUrl.foreach { url =>
      out.println(scala.io.Source.fromURL(url).mkString)
    }

  private lazy val formatImport: String => String =
    sys.props.get("scala-ts.printer.import-pattern") match {
      case Some(pattern) =>
        pattern.format(_: String)

      case _ => { tpeName: String => s"{ ${tpeName}, is${tpeName} }" }
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
    import settings.{ typescriptLineSeparator => sep }

    val typeNaming = settings.typeNaming(settings, _: TypeRef)

    requires.toList.sortBy(_.name).foreach { tpe =>
      val tpeName = typeNaming(tpe)

      out.println(
        s"import ${formatImport(tpeName)} from '${importPath(tpe)}'${sep}"
      )
    }

    if (requires.nonEmpty) {
      out.println()
    }
  }
}
