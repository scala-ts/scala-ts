package com.mpc.scalats.sbt

import java.io.PrintStream
import java.net.URLClassLoader

import com.mpc.scalats.configuration.Config
import com.mpc.scalats.core.TypeScriptGenerator
import sbt.Keys._
import sbt._
import complete.DefaultParsers._

object TypeScriptGeneratorPlugin extends AutoPlugin {

  object autoImport {
    val generateTypeScript = inputKey[Unit]("Generate Type Script")

    val emitInterfaces = settingKey[Boolean]("Generate interface declarations")
    val emitClasses = settingKey[Boolean]("Generate class declarations")
    val optionToNullable =
      settingKey[Boolean]("Option types will be compiled to 'type | null'")
    val optionToUndefined =
      settingKey[Boolean]("Option types will be compiled to 'type | undefined'")
    val outputFile = settingKey[Option[PrintStream]](
      "Print stream to write. Defaults to Console.out")
    val prependIPrefix =
      settingKey[Boolean]("Whether to prefix interface names with I")
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
        (prependIPrefix in generateTypeScript).value
      )

      val args = spaceDelimited("").parsed
      val cp: Seq[File] = (fullClasspath in Runtime).value.files
      val cpUrls = cp.map(_.asURL).toArray
      val cl = new URLClassLoader(cpUrls, ClassLoader.getSystemClassLoader)

      TypeScriptGenerator.generateFromClassNames(args.toList, cl)
    },
    emitInterfaces in generateTypeScript := true,
    emitClasses in generateTypeScript := false,
    optionToNullable in generateTypeScript := true,
    optionToUndefined in generateTypeScript := false,
    outputFile in generateTypeScript := None,
    prependIPrefix := false
  )

}
