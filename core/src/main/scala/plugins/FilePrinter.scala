package io.github.scalats.plugins

import java.io.{ File, FileOutputStream, PrintStream }

import io.github.scalats.core.Settings
import io.github.scalats.typescript.{ Declaration, TypeRef }

// TODO: Printer that gather class and interface (need to pass `Declaration` or `TypeRef` instead of just `name: String`)
final class FilePrinter(outDir: File) extends PrinterWithPrelude {

  def apply(
    conf: Settings,
    kind: Declaration.Kind,
    name: String,
    requires: Set[TypeRef]): PrintStream = {

    import conf.{ typescriptLineSeparator => sep }

    val f = new File(outDir, s"${name}.ts")
    val stream = new PrintStream(new FileOutputStream(f, true))

    printPrelude(stream)

    requires.foreach {
      case TypeRef.Named(n) =>
        stream.println(s"import { ${n} } from './${n}'${sep}")

      case _ =>
        ()
    }

    if (requires.nonEmpty) {
      stream.println()
    }

    stream
  }
}
