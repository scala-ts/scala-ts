package io.github.scalats.core

import java.io.PrintStream

trait TypeScriptPrinter extends (String => PrintStream) {
  def apply(name: String): PrintStream
}

object TypeScriptPrinter {
  object StandardOutput extends TypeScriptPrinter {
    def apply(name: String) = Console.out
  }
}
