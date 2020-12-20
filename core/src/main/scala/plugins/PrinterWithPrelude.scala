package io.github.scalats.plugins

import java.io.PrintStream

import io.github.scalats.core.TypeScriptPrinter

abstract class PrinterWithPrelude extends TypeScriptPrinter {
  /**
   * If the system property `scala-ts.printer.prelude-url` is defined,
   * then print the content from the URL as prelude to the given stream.
   */
  protected def printPrelude(out: PrintStream): Unit =
    sys.props.get("scala-ts.printer.prelude-url").foreach { url =>
      out.println(scala.io.Source.fromURL(url).mkString)
    }
}
