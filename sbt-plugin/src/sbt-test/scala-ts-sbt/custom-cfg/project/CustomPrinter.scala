package scalats

import java.io.{ File, FileOutputStream, PrintStream }

final class CustomPrinter(outDir: File)
  extends io.github.scalats.core.TypeScriptPrinter {

  def apply(name: String): PrintStream = {
    val n = name.stripPrefix("I") // Strip interface 'I' prefix

    new PrintStream(new FileOutputStream(
      new File(outDir, s"scalats${n}.ts"), true))
  }
}
