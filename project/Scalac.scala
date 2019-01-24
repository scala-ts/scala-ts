// compiler options
import sbt.Keys._
import sbt._

object Scalac {
  lazy val settings = Seq(
    scalacOptions ++= {
      val opts = Seq(
        "-encoding", "UTF-8",
        "-target:jvm-1.8",
        "-unchecked",
        "-deprecation",
        "-feature",
        "-Xfatal-warnings",
        "-Xlint",
        "-Ywarn-numeric-widen",
        "-Ywarn-dead-code",
        "-Ywarn-value-discard",
        "-Yno-adapted-args",
        "-Ywarn-inaccessible",
        "-Ywarn-nullary-override",
        "-Ywarn-nullary-unit",
        "-g:vars"
      )

      if (scalaBinaryVersion.value == "2.12") {
        opts ++ Seq(
          "-Ywarn-unused",
          "-Ywarn-unused-import",
          "-Ywarn-infer-any",
          "-Ywarn-extra-implicit"
        )
      } else {
        opts
      }
    },
    scalacOptions in (Compile, console) ~= {
      _.filterNot { opt => opt.startsWith("-X") || opt.startsWith("-Y") }
    },
    scalacOptions in (Test, console) ~= {
      _.filterNot { opt => opt.startsWith("-X") || opt.startsWith("-Y") }
    }
  )
}
