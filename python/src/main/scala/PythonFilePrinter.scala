package io.github.scalats.python

import java.io.{ File, FileOutputStream, PrintStream }

import io.github.scalats.core.Internals.ListSet
import io.github.scalats.core.Settings
import io.github.scalats.plugins.BasePrinter
import io.github.scalats.typescript.{ Declaration, TypeRef, SingletonTypeRef }

final class PythonFilePrinter(outDir: File) extends BasePrinter {
  private val tracker = scala.collection.mutable.Map.empty[String, File]

  @com.github.ghik.silencer.silent(".*kind.*never used.*")
  def apply(
      conf: Settings,
      kind: Declaration.Kind,
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

    if (kind == Declaration.Interface) {
      stream.println("from dataclasses import dataclass")
    } else if (kind == Declaration.Singleton) {
      stream.println("from dataclasses import dataclass  # noqa: F401")
    }

    stream.println("""import typing  # noqa: F401
import datetime  # noqa: F401
""")

    printImports(conf, kind, requires, stream)

    if (requires.nonEmpty) {
      stream.println()
    }

    stream.println()

    stream
  }

  private def printImports(
      settings: Settings,
      kind: Declaration.Kind,
      requires: ListSet[TypeRef],
      out: PrintStream
    ): Unit = {
    val typeNaming = settings.typeNaming(settings, _: TypeRef)
    val requiredTypes = requires.toList.sortBy(_.name)

    requiredTypes.foreach { tpe =>
      val tpeName = typeNaming(tpe)
      val mod = tpeName.toLowerCase

      tpe match {
        case _: SingletonTypeRef if (kind != Declaration.Union) =>
          out.println(s"import ${mod}  # Singleton")

        case _ =>
          out.println(s"""import ${mod}  # noqa: F401
from ${mod} import ${tpeName}""")
      }
    }
  }
}
