package org.scalats.core

import java.io.PrintStream

trait TypeScriptPrinter extends (String => PrintStream) {
  def apply(name: String): PrintStream
}

object TypeScriptPrinter {
  lazy val StandardOutput = new TypeScriptPrinter {
    def apply(name: String) = Console.out
  }
}
