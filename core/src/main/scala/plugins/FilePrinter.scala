package io.github.scalats.plugins

import java.io.{ File, PrintStream }

final class FilePrinter(outDir: File)
  extends io.github.scalats.core.TypeScriptPrinter {

  def apply(name: String): PrintStream =
    new PrintStream(new File(outDir, s"${name}.ts"))
}
