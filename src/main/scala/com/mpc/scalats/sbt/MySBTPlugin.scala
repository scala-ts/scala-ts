package com.mpc.scalats.sbt

import java.net.URLClassLoader

import com.mpc.scalats.core.TypeScriptGenerator
import sbt.Keys._
import sbt._
import sbt.complete.DefaultParsers._

object MySBTPlugin extends AutoPlugin {

  object autoImport {
    val hello = inputKey[Unit]("Says hello")
  }

  import autoImport._

  override lazy val projectSettings = Seq(
    hello := {
      val args = spaceDelimited("").parsed
      System.out.println(s"Hello, ${args(0)}")
    }
  )
}
