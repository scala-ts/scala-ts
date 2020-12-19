package io.github.scalats.plugins

import java.io.{ File, FileOutputStream, PrintStream }

import io.github.scalats.typescript.TypeRef

// TODO: Printer that gather class and interface (need to pass `Declaration` or `TypeRef` instead of just `name: String`)
final class FilePrinter(outDir: File) extends PrinterWithPrelude {

  def apply(name: String, requires: Set[TypeRef]): PrintStream = {
    // TODO: Manager import according `requires`

    val f = new File(outDir, s"${name}.ts")

    new PrintStream(new FileOutputStream(f, true))
  }
}
