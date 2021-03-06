import sbt.Keys._
import sbt._
import sbt.plugins.JvmPlugin

object Compiler extends AutoPlugin {
  override def trigger = allRequirements
  override def requires = JvmPlugin

  override lazy val projectSettings = Seq(
    scalaVersion := "2.12.12",
    crossScalaVersions := Seq(scalaVersion.value),
    crossVersion := CrossVersion.binary,
    scalacOptions ++= Seq(
      "-encoding", "UTF-8", "-target:jvm-1.8",
      "-unchecked",
      "-deprecation",
      "-feature",
      "-Xlint",
      "-g:vars",
      "-Xfatal-warnings"
    ),
    scalacOptions ++= {
      val ver = scalaBinaryVersion.value

      if (ver == "2.11") {
        Seq(
          "-Xmax-classfile-name", "128",
          "-Yopt:_", "-Ydead-code", "-Yclosure-elim", "-Yconst-opt"
        )
      } else if (ver == "2.12") {
        Seq(
          "-Xmax-classfile-name", "128",
          "-Ywarn-numeric-widen",
          "-Ywarn-dead-code",
          "-Ywarn-value-discard",
          "-Ywarn-infer-any",
          "-Ywarn-unused",
          "-Ywarn-unused-import",
          "-Ywarn-macros:after"
        )
      } else {
        Seq(
          "-explaintypes",
          "-Werror",
          "-Wnumeric-widen",
          "-Wdead-code",
          "-Wvalue-discard",
          "-Wextra-implicit",
          "-Wmacros:after",
          "-Wunused")
      }
    },
    scalacOptions in (Compile, console) ~= { _.filterNot(excludeScalacOpts) },
    scalacOptions in (Compile, doc) ~= { _.filterNot(excludeScalacOpts) },
    libraryDependencies ++= {
      val silencerVersion = "1.7.1"

      Seq(
        compilerPlugin(("com.github.ghik" %% "silencer-plugin" % silencerVersion).
          cross(CrossVersion.full)),
        ("com.github.ghik" %% "silencer-lib" % silencerVersion % Provided).
          cross(CrossVersion.full)
      )
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
