package scalats

import scala.collection.immutable.Set

import java.io.{ File, FileOutputStream, PrintStream }

import io.github.scalats.typescript.{ Declaration, TypeRef }
import io.github.scalats.plugins.PrinterWithPrelude

final class CustomPrinter(outDir: File) extends PrinterWithPrelude {
  @volatile private var first = true

  def apply(
    conf: io.github.scalats.core.Settings,
    kind: Declaration.Kind,
    name: String,
    requires: Set[TypeRef]): PrintStream = {

    val writePrelude: Boolean = {
      if (first) {
        first = false
        true
      } else {
        false
      }
    }

    val out = new PrintStream(new FileOutputStream(
      new File(outDir, s"scalats${name}.ts"), true))

    if (writePrelude) {
      printPrelude(out)
    }

    val typeNaming = conf.typeNaming(conf, _: TypeRef)
    import conf.{ typescriptLineSeparator => sep }

    requires.foreach { tpe =>
      val tpeName = typeNaming(tpe)

      out.println(s"import { ${tpeName} } from './${tpeName}'${sep}")
    }

    if (requires.nonEmpty) {
      out.println()
    }

    out
  }
}
