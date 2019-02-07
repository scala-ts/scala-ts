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
    val interfacePrefix = settingKey[String]("Interfaces prefix")
    val optionToNullable = settingKey[Boolean]("Option types will be compiled to 'type | null'")
    val optionToUndefined = settingKey[Boolean]("Option types will be compiled to 'type | undefined'")
    val outputStream = settingKey[Option[PrintStream]]("Print stream to write. Defaults to Console.out")
    val customNameMap = settingKey[Map[String, String]]("Custom names mapping for classes. Doesn't allow to override standard types. Names will not use interfacePrefix setting.")
    val leafTypes = settingKey[Set[String]]("Forced leaf types for parsing. The parser won't explore involved types for those types, and won't emit them. They'll still be used as member types where they are involved.")
  }

  import autoImport._

  override lazy val projectSettings = Seq(
    generateTypeScript := {
      implicit val config = Config(
        (interfacePrefix in generateTypeScript).value,
        (optionToNullable in generateTypeScript).value,
        (optionToUndefined in generateTypeScript).value,
        (outputStream in generateTypeScript).value,
        (customNameMap in generateTypeScript).value,
        (leafTypes in generateTypeScript).value,
      )

      val args = spaceDelimited("").parsed
      val cp: Seq[File] = (fullClasspath in Runtime).value.files
      val cpUrls = cp.map(_.asURL).toArray
      val cl = new URLClassLoader(cpUrls, ClassLoader.getSystemClassLoader)

      TypeScriptGenerator.generateFromClassNames(args.toList, cl)
    },
    interfacePrefix in generateTypeScript := "IElium",
    optionToNullable in generateTypeScript := true,
    optionToUndefined in generateTypeScript := false,
    outputStream in generateTypeScript := None,
    customNameMap in generateTypeScript := Map("Metric" -> "string"),
    leafTypes in generateTypeScript := Set.empty
  )

}
