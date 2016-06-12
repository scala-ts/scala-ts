package com.mpc.scalats.sbt

import java.net.URLClassLoader

import com.mpc.scalats.core.TypeScriptGenerator
import sbt.Keys._
import sbt._
import complete.DefaultParsers._

object TypeScriptGeneratorPlugin extends AutoPlugin {

  object autoImport {
    val generateTypeScript = inputKey[Unit]("Generate Type Script")
  }

  import autoImport._

  override lazy val projectSettings = Seq(
    generateTypeScript := {
      val args = spaceDelimited("").parsed
      val cp: Seq[File] = (fullClasspath in Runtime).value.files
      val cpUrls = cp.map(_.asURL).toArray
      val cl = new URLClassLoader(cpUrls, ClassLoader.getSystemClassLoader)

      TypeScriptGenerator.generateFromClassNames(args.toList, System.out, cl)
    }
  )

}
