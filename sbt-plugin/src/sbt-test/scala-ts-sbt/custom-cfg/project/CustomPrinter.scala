package scalats

import scala.collection.immutable.Set

import java.io.{ File, FileOutputStream, PrintStream }

import io.github.scalats.typescript.TypeRef
import io.github.scalats.plugins.PrinterWithPrelude

final class CustomPrinter(outDir: File) extends PrinterWithPrelude {
  @volatile private var first = true

  def apply(name: String, requires: Set[TypeRef]): PrintStream = {
    val n = name.stripPrefix("I") // Strip interface 'I' prefix

    val writePrelude: Boolean = {
      if (first) {
        first = false
        true
      } else {
        false
      }
    }

    val out = new PrintStream(new FileOutputStream(
      new File(outDir, s"scalats${n}.ts"), true))

    if (writePrelude) {
      printPrelude(out)
    }

    out
  }
}
