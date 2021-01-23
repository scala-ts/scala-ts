package io.github.scalats.plugins

import java.io.{ File, FileOutputStream, PrintStream }

import io.github.scalats.core.Settings
import io.github.scalats.typescript.{ Declaration, TypeRef }

// TODO: Printer that gather class and interface
final class FilePrinter(outDir: File) extends BasePrinter {

  def apply(
    conf: Settings,
    kind: Declaration.Kind,
    name: String,
    requires: Set[TypeRef]): PrintStream = {

    val f = new File(outDir, s"${name}.ts")
    val stream = new PrintStream(new FileOutputStream(f, true))

    printPrelude(stream)

    printImports(conf, requires, stream) { tpe =>
      s"./${tpe.name}"
    }

    stream
  }
}
