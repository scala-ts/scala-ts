package scalats

import scala.collection.immutable.ListSet

import java.io.{ File, FileOutputStream, PrintStream }

import io.github.scalats.typescript.{ Declaration, TypeRef }
import io.github.scalats.plugins.BasePrinter

final class CustomPrinter(outDir: File) extends BasePrinter {

  def apply(
      conf: io.github.scalats.core.Settings,
      kind: Declaration.Kind,
      name: String,
      requires: ListSet[TypeRef]
    ): PrintStream = {

    val writePrelude: Boolean = {
      if (name == "Bar") {
        true
      } else {
        false
      }
    }

    val out = new PrintStream(
      new FileOutputStream(new File(outDir, s"scalats${name}.ts"), true)
    )

    if (writePrelude) {
      printPrelude(out)
    }

    printImports(conf, requires, out) { tpe => s"./scalats${tpe.name}" }

    out
  }
}
