package scalats

import java.io.{ File, PrintStream }

final class CustomPrinter(outDir: File)
  extends io.github.scalats.core.TypeScriptPrinter {

  def apply(name: String): PrintStream =
    new PrintStream(new File(outDir, s"scalats${name}.ts"))
}
