package io.github.scalats.python

import java.io.{ File, FileOutputStream, PrintStream }

import io.github.scalats.ast.{ Declaration, SingletonTypeRef, TypeRef }
import io.github.scalats.core.Internals.ListSet
import io.github.scalats.core.Settings
import io.github.scalats.plugins.BasePrinter

final class PythonFilePrinter(outDir: File) extends BasePrinter {
  private val tracker = scala.collection.mutable.Map.empty[String, File]

  private lazy val baseModule: Option[String] =
    sys.props.get("scala-ts.printer.python-base-module").filter(_.nonEmpty)

  @com.github.ghik.silencer.silent(".*(kind|others).*never used.*")
  def apply(
      conf: Settings,
      kind: Declaration.Kind,
      others: ListSet[Declaration.Kind],
      name: String,
      requires: ListSet[TypeRef]
    ): PrintStream = {
    val f = tracker.getOrElseUpdate(
      name, {
        val n = new File(outDir, s"${name.toLowerCase}.py")

        // Make sure it's cleaned before the first output
        n.delete()

        n
      }
    )

    val stream = new PrintStream(new FileOutputStream(f, true))

    printPrelude(stream)

    if (
      kind == Declaration.Interface || others.contains(Declaration.Interface)
    ) {
      stream.println("from dataclasses import dataclass")
    } else if (
      kind == Declaration.Singleton || others.contains(Declaration.Singleton)
    ) {
      stream.println("from dataclasses import dataclass  # noqa: F401")
    }

    stream.println("""import typing  # noqa: F401
import datetime  # noqa: F401
import time  # noqa: F401
""")

    if (requires.nonEmpty) {
      baseModule match {
        case Some(base) =>
          printImports(conf, base + '.', kind, requires, stream) { mod =>
            s"from ${base} import ${mod}"
          }

        case None =>
          printImports(conf, "", kind, requires, stream) { mod =>
            s"import ${mod}"
          }
      }

      stream.println()
    }

    stream.println()

    stream
  }

  private def printImports(
      settings: Settings,
      tpePrefix: String,
      kind: Declaration.Kind,
      requires: ListSet[TypeRef],
      out: PrintStream
    )(importModule: String => String
    ): Unit = {
    val typeNaming = settings.typeNaming(settings, _: TypeRef)
    val requiredImports = requires.map { tpe =>
      val tpeName = typeNaming(tpe)
      val mod = tpeName.toLowerCase

      tpe match {
        case _: SingletonTypeRef if (kind != Declaration.Union) =>
          s"${importModule(mod)}  # Singleton"

        case _ =>
          s"""${importModule(mod)}  # noqa: F401
from ${tpePrefix}${mod} import ${tpeName}"""
      }
    }

    requiredImports.toList.sorted.foreach(out.println)
  }
}
