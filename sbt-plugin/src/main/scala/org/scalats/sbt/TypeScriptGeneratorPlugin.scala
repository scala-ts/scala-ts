package org.scalats.sbt

import java.io.PrintStream
import java.net.URLClassLoader

import org.scalats.configuration.{ Config, FieldNaming }
import org.scalats.core.{ Logger, TypeScriptGenerator }
import sbt.Keys._
import sbt._
import complete.DefaultParsers._

object TypeScriptGeneratorPlugin extends AutoPlugin {
  object autoImport {
    val generateTypeScript = inputKey[Unit]("Generate TypeScript")

    val emitInterfaces = settingKey[Boolean]("Generate interface declarations")
    val emitClasses = settingKey[Boolean]("Generate class declarations")
    val optionToNullable = settingKey[Boolean]("Option types will be compiled to 'type | null'")
    val optionToUndefined = settingKey[Boolean]("Option types will be compiled to 'type | undefined'")
    val outputFile  = settingKey[Option[PrintStream]]("Print stream to write. Defaults to Console.out")
    val prependIPrefix = settingKey[Boolean]("Whether to prefix interface names with I")
    val prependEnclosingClassNames = settingKey[Boolean]("Whether to prepend enclosing class/object names")
    val typescriptIndent = settingKey[String]("Characters used as TypeScript indentation (e.g. \\t)")
    val emitCodecs = settingKey[Boolean]("Generate the codec functions fromData/toData for TypeScript classes")
    val fieldNaming = settingKey[FieldNaming]("Conversions for the field names if emitCodecs (default: FieldNaming.Identity)")

    // TODO: includes/excludes
  }

  import autoImport._

  override lazy val projectSettings = Seq(
    generateTypeScript := {
      implicit val config = Config(
        (emitInterfaces in generateTypeScript).value,
        (emitClasses in generateTypeScript).value,
        (optionToNullable in generateTypeScript).value,
        (optionToUndefined in generateTypeScript).value,
        (outputFile in generateTypeScript).value,
        (prependIPrefix in generateTypeScript).value,
        (prependEnclosingClassNames in generateTypeScript).value,
        (typescriptIndent in generateTypeScript).value,
        (emitCodecs in generateTypeScript).value,
        (fieldNaming in generateTypeScript).value
      )

      val args = spaceDelimited("").parsed
      val cp: Seq[File] = (fullClasspath in Runtime).value.files
      val cpUrls = cp.map(_.asURL).toArray
      val cl = new URLClassLoader(cpUrls, ClassLoader.getSystemClassLoader)

      TypeScriptGenerator.generateFromClassNames(
        args.toList, logger(streams.value.log), cl)
    },
    emitInterfaces in generateTypeScript := true,
    emitClasses in generateTypeScript := false,
    optionToNullable in generateTypeScript := true,
    optionToUndefined in generateTypeScript := false,
    outputFile in generateTypeScript := None,
    prependIPrefix := false,
    prependEnclosingClassNames in generateTypeScript := false,
    typescriptIndent in generateTypeScript := "\t",
    emitCodecs in generateTypeScript := true,
    fieldNaming in generateTypeScript := FieldNaming.Identity
  )

  // ---

  import scala.language.reflectiveCalls

  private def logger(l: SbtLogger) = new Logger {
    def warning(msg: => String): Unit = l.warn(msg)
  }

  private type SbtLogger = {
    def warn(msg: => String): Unit
  }
}
