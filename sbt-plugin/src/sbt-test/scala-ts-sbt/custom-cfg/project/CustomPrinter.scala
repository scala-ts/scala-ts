package scalats

import java.io.{ File, PrintStream }

final class CustomPrinter(outDir: File)
  extends org.scalats.core.TypeScriptPrinter {

  def apply(name: String): PrintStream =
    new PrintStream(new File(outDir, s"scalats${name}.ts"))
}
