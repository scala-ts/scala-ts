import sbt.Keys._

lazy val publishSettings = Seq(
  publishMavenStyle := true,
  publishTo <<= version { (v: String) =>
    val nexus = "https://nexus.elium.io/repository/"
    if (v.trim.endsWith("SNAPSHOT"))
      Some("Snapshots" at nexus + "maven-snapshots")
    else
      Some("Releases" at nexus + "maven-releases")
  },
  credentials += Credentials(new File("/root") / ".ivy2" / ".credentials"),
  credentials += Credentials(Path.userHome / ".ivy2" / ".credentials")
)


lazy val projectSettings = Seq(
  name := "scala-ts",
  version := "0.4.1",
  organization := "com.github.miloszpp",
  scalaVersion := "2.10.6",
  mainClass in (Compile, run) := Some("com.mpc.scalats.Main"),
  sbtPlugin := true,
  sbtVersion := "0.13.11"
)

lazy val root = project.in(file("."))
  .settings(projectSettings, publishSettings)

libraryDependencies ++= Seq(
  "org.scala-lang" % "scala-reflect" % "2.10.6",
  "ch.qos.logback" % "logback-classic" % "1.1.7",
  "org.scalatest" %% "scalatest" % "3.0.1" % "test"
)
