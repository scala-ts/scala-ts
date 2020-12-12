package org.scalats.plugins

import java.io.{ File, PrintStream }

final class FilePrinter(outDir: File)
  extends org.scalats.core.TypeScriptPrinter {

  def apply(name: String): PrintStream =
    new PrintStream(new File(outDir, s"${name}.ts"))
}
