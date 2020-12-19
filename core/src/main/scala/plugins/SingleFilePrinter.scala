package io.github.scalats.plugins

import java.io.{ File, FileOutputStream, PrintStream }

import io.github.scalats.typescript.{ Declaration, TypeRef }

/**
 * Prints all the generated code the a single file.
 *
 * The default file name is `scala.ts`.
 * A custom file name can be specified using the `scala-ts.single-filename`
 * system property.
 */
final class SingleFilePrinter(outDir: File) extends PrinterWithPrelude {

  private val flag = new java.util.concurrent.atomic.AtomicBoolean(false)

  private val tracker = scala.collection.mutable.Map.
    empty[(Declaration.Kind, String), None.type]

  private lazy val filename = sys.props.getOrElse(
    "scala-ts.single-filename", "scala.ts")

  def apply(
    kind: Declaration.Kind,
    name: String,
    requires: Set[TypeRef]): PrintStream = {

    val f = new File(outDir, filename)

    val append: Boolean = {
      if (!flag.getAndSet(true)) {
        f.delete() // For the first type, clean file from a prior compilation
        false
      } else {
        true
      }
    }

    var fresh: Boolean = false

    tracker.getOrElseUpdate(kind -> name, {
      fresh = true
      None
    })

    def fileStream = {
      new PrintStream(new FileOutputStream(f, append))
    }

    if (!append) {
      val stream = fileStream

      printPrelude(stream)

      stream
    } else if (fresh) {
      fileStream
    } else {
      // Skip already output'ed declaration
      new PrintStream(io.github.scalats.core.NullOutputStream)
    }
  }
}
