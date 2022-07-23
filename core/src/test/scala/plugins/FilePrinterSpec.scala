package io.github.scalats.plugins

import java.io.{ File, PrintStream }

import scala.io.Source.fromFile

import io.github.scalats.core.Internals.ListSet
import io.github.scalats.typescript.{ CustomTypeRef, Declaration, TypeRef }

final class FilePrinterSpec extends org.specs2.mutable.Specification {
  "File printer".title

  sequential

  "Single file printer" should {
    sequential

    def withTemp[T](name: String)(spec: File => T): T = {
      val tmp = File.createTempFile("FilePrinterSpec", "")
      tmp.delete()
      tmp.mkdirs()

      val file = new File(tmp, name)
      val p = new SingleFilePrinter(tmp)
      val printer = {
        val conf = io.github.scalats.core.Settings()
        p(conf, Declaration.Class, _: String, _: ListSet[TypeRef])
      }

      try {
        withPrinter(new PrintStream(file)) { p1 =>
          p1.println("_prior")

          withPrinter(printer("foo", ListSet.empty)) { p2 =>
            p2.println("FOO")

            withPrinter(
              printer("bar", ListSet(CustomTypeRef("Foo", List.empty)))
            ) { p3 =>
              p3.println("BAR")
              p3.flush()

              spec(file)
            }
          }
        }
      } finally {
        if (file.exists) {
          file.delete()
        }

        ()
      }
    }

    "output to the default file" in withTemp("scala.ts") { file =>
      file.getName must_=== "scala.ts" and {
        fromFile(
          file
        ).mkString.replace("\r", "") must_=== """declare var exports: any;

export const nsfoo = exports;

FOO

export const nsbar = exports;

BAR
"""
      }
    }

    "output to a custom file" in {
      withProp("scala-ts.single-filename", "single.ts") {
        withTemp("single.ts") { file =>
          file.getName must_=== "single.ts" and {
            fromFile(
              file
            ).mkString.replace("\r", "") must_=== """declare var exports: any;

export const nsfoo = exports;

FOO

export const nsbar = exports;

BAR
"""
          }
        }
      }
    }

    "output with a custom prelude" in {
      val prelude = File.createTempFile("FilePrinterSpec", ".prelude")

      withPrinter(new PrintStream(prelude)) { out =>
        out.println("// Prelude\n// ...")
        out.flush()

        withProp("scala-ts.printer.prelude-url", prelude.toURI.toString) {
          withTemp("scala.ts") { file =>
            file.getName must_=== "scala.ts" and {
              fromFile(
                file
              ).mkString.replace("\r", "") must_=== """// Prelude
// ...

declare var exports: any;

export const nsfoo = exports;

FOO

export const nsbar = exports;

BAR
"""
            }
          }
        }
      }
    }
  }

  // ---

  private def withProp[T](key: String, value: String)(f: => T): T = {
    try {
      sys.props.put(key, value)

      f
    } finally {
      sys.props.remove(key)

      ()
    }
  }

  private def withPrinter[T](p: => PrintStream)(f: PrintStream => T): T = {
    var w: PrintStream = null

    try {
      w = p
      f(w)
    } finally {
      if (w != null) {
        w.close()
      }
    }
  }
}
