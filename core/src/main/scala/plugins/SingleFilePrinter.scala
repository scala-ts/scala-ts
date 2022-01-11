package io.github.scalats.plugins

import java.io.{ File, FileOutputStream, PrintStream }

import scala.collection.immutable.ListSet

import io.github.scalats.core.Settings
import io.github.scalats.typescript.{ Declaration, TypeRef }

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

    val stream = new PrintStream(new FileOutputStream(f, append))

    if (!append) {
      printPrelude(stream)
    } else {
      stream.println()
    }

    stream
  }
}
