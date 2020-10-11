import sbt.Keys._
import sbt._
import sbt.plugins.JvmPlugin

object Compiler extends AutoPlugin {
  override def trigger = allRequirements
  override def requires = JvmPlugin

  override def projectSettings= Seq(
    scalaVersion := "2.12.12",
    crossScalaVersions := Seq("2.11.12", scalaVersion.value, "2.13.3"),
    unmanagedSourceDirectories in Compile += {
      val base = (sourceDirectory in Compile).value

      CrossVersion.partialVersion(scalaVersion.value) match {
        case Some((2, n)) if n >= 12 => base / "scala-2.12+"
        case _                       => base / "scala-2.12-"
      }
    },
    scalacOptions ++= {
      val opts = Seq(
        "-encoding", "UTF-8",
        "-target:jvm-1.8",
        "-unchecked",
        "-deprecation",
        "-feature",
        //"-Xfatal-warnings", // Issue on publish due to annotation
        "-Xlint",
        "-Ywarn-numeric-widen",
        "-Ywarn-dead-code",
        "-Ywarn-value-discard",
        "-g:vars"
      )

      if (scalaBinaryVersion.value == "2.12") {
        opts ++ Seq(
          "-Ywarn-nullary-override",
          "-Ywarn-nullary-unit",
          "-Ywarn-inaccessible",
          "-Yno-adapted-args",
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
