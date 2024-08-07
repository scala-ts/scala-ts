package io.github.scalats.plugins

import java.io.{ File, FileOutputStream, PrintStream }

import io.github.scalats.ast.{ Declaration, TypeRef }
import io.github.scalats.core.Internals.ListSet
import io.github.scalats.core.Settings

/**
 * Prints all the generated code the a single file.
 *
 * The default file name is `scala.ts`.
 * A custom file name can be specified using the `scala-ts.single-filename`
 * system property.
 */
final class SingleFilePrinter(outDir: File) extends BasePrinter {

  private val flag = new java.util.concurrent.atomic.AtomicBoolean(false)

  private lazy val filename =
    sys.props.getOrElse("scala-ts.single-filename", "scala.ts")

  def apply(
      conf: Settings,
      kind: Declaration.Kind,
      others: ListSet[Declaration.Kind],
      name: String,
      requires: ListSet[TypeRef]
    ): PrintStream = {

    val f = new File(outDir, filename)

    val append: Boolean = {
      if (!flag.getAndSet(true)) {
        f.delete() // For the first type, clean file from a prior compilation
        false
      } else {
        true
      }
    }

    import conf.{ lineSeparator => lineSep }

    val stream = new PrintStream(new FileOutputStream(f, append))

    // For module compatibility & self reference
    if (!append) {
      printPrelude(stream)

      stream.println(s"declare var exports: any${lineSep}\r\n")
    } else {
      stream.println()
    }

    stream.println(s"export const ns${name} = exports${lineSep}\r\n")

    stream
  }
}
