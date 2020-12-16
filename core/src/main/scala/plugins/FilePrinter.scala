package io.github.scalats.plugins

import java.io.{ File, FileOutputStream, PrintStream }

final class FilePrinter(outDir: File)
  extends io.github.scalats.core.TypeScriptPrinter {

  def apply(name: String): PrintStream = {
    val f = new File(outDir, s"${name}.ts")

    new PrintStream(new FileOutputStream(f, true))
  }
}
