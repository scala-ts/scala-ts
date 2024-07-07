package io.github.scalats.plugins

import java.io.{ File, FileOutputStream, PrintStream }

import io.github.scalats.ast.{ Declaration, TypeRef }
import io.github.scalats.core.Internals.ListSet
import io.github.scalats.core.Settings

// TODO: Printer that gather class and interface
final class FilePrinter(outDir: File) extends BasePrinter {
  private val tracker = scala.collection.mutable.Map.empty[String, File]

  @com.github.ghik.silencer.silent(".*kind.*never used.*")
  def apply(
      conf: Settings,
      kind: Declaration.Kind,
      others: ListSet[Declaration.Kind],
      name: String,
      requires: ListSet[TypeRef]
    ): PrintStream = {
    val f = tracker.getOrElseUpdate(
      name, {
        val n = new File(outDir, s"${name}.ts")

        // Make sure it's cleaned before the first output
        n.delete()

        n
      }
    )

    import conf.{ lineSeparator => lineSep }

    val stream = new PrintStream(new FileOutputStream(f, true))

    printPrelude(stream)

    // For module compatibility & self reference
    stream.println(s"""declare var exports: any${lineSep}

export const ns${name} = exports${lineSep}
""")

    printImports(conf, requires, stream) { tpe => s"./${tpe.name}" }

    if (requires.nonEmpty) {
      val typeNaming = conf.typeNaming(conf, _: TypeRef)
      val depMods = requires.map { tpe =>
        // Required can contain different types with same name but not same kind
        // (e.g. class and its companion object), which must be merge as single import there

        s"${conf.indent}ns${typeNaming(tpe)},"
      }

      stream.println("""
export const dependencyModules = [""")

      depMods.toList.sorted.foreach(stream.println)

      stream.println(s"""]${lineSep}
""")
    }

    stream
  }
}
