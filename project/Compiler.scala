import sbt.Keys._
import sbt._
import sbt.plugins.JvmPlugin

import cchantep.HighlightExtractorPlugin.autoImport.{
  highlightActivation,
  HLEnabledBySysProp
}

object Compiler extends AutoPlugin {
  override def trigger = allRequirements
  override def requires = JvmPlugin

  override lazy val projectSettings = Seq(
    scalaVersion := "2.12.16",
    crossScalaVersions := Seq(scalaVersion.value),
    crossVersion := CrossVersion.binary,
    scalacOptions ++= Seq(
      "-encoding",
      "UTF-8",
      "-unchecked",
      "-deprecation",
      "-feature",
      "-Xfatal-warnings"
    ),
    scalacOptions ++= {
      if (scalaBinaryVersion.value == "3") {
        Seq.empty
      } else {
        Seq("-target:jvm-1.8", "-Xlint", "-g:vars")
      }
    },
    scalacOptions ++= {
      val sv = scalaBinaryVersion.value

      if (sv == "2.12") {
        Seq(
          "-Xmax-classfile-name",
          "128",
          "-Ywarn-numeric-widen",
          "-Ywarn-dead-code",
          "-Ywarn-value-discard",
          "-Ywarn-infer-any",
          "-Ywarn-unused",
          "-Ywarn-unused-import",
          "-Ywarn-macros:after"
        )
      } else if (sv == "2.11") {
        Seq(
          "-Xmax-classfile-name",
          "128",
          "-Yopt:_",
          "-Ydead-code",
          "-Yclosure-elim",
          "-Yconst-opt"
        )
      } else if (sv == "2.13") {
        Seq(
          "-explaintypes",
          "-Werror",
          "-Wnumeric-widen",
          "-Wdead-code",
          "-Wvalue-discard",
          "-Wextra-implicit",
          "-Wmacros:after",
          "-Wunused"
        )
      } else {
        Seq("-Wunused:all", "-language:implicitConversions")
      }
    },
    Compile / console / scalacOptions ~= { _.filterNot(excludeScalacOpts) },
    Compile / doc / scalacOptions ~= { _.filterNot(excludeScalacOpts) },
    highlightActivation := HLEnabledBySysProp("highlight"),
    libraryDependencies ++= {
      if (scalaBinaryVersion.value != "3") {
        val silencerVersion = "1.7.11"

        Seq(
          compilerPlugin(
            ("com.github.ghik" %% "silencer-plugin" % silencerVersion)
              .cross(CrossVersion.full)
          ),
          ("com.github.ghik" %% "silencer-lib" % silencerVersion % Provided)
            .cross(CrossVersion.full)
        )
      } else Seq.empty
    }
  )

  private val excludeTestScalacOpts: String => Boolean = { o =>
    o.startsWith("-X") || o.startsWith("-Y")
  }

  private val excludeScalacOpts: String => Boolean = { o =>
    excludeTestScalacOpts(o) || o == "128" ||
    o.startsWith("-P:silencer") || o.startsWith("-P:semanticdb") ||
    // skip fatal warning as silencer is not enabled
    o == "-Werror" || o == "-Xfatal-warnings"
  }
}
