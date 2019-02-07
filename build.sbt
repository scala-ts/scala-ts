import sbt.Keys._

lazy val publishSettings = Seq(
  publishMavenStyle := true,
  publishTo := {
    val nexus = "https://nexus.elium.io/repository/"
    if (isSnapshot.value)
      Some("Snapshots" at nexus + "maven-snapshots")
    else
      Some("Releases" at nexus + "maven-releases")
  },
  credentials += Credentials(new File("/root") / ".ivy2" / ".credentials"),
  credentials += Credentials(Path.userHome / ".ivy2" / ".credentials")
)


lazy val projectSettings = Seq(
  name := "scala-ts",
  version := "1.0.0",
  organization := "com.github.miloszpp",
  scalaVersion := "2.12.6",
  mainClass in (Compile, run) := Some("com.mpc.scalats.Main"),
  sbtPlugin := true,
  sbtVersion := "1.1.5"
)

lazy val root = project.in(file("."))
  .settings(projectSettings, publishSettings)

libraryDependencies ++= Seq(
  "org.scala-lang" % "scala-reflect" % "2.12.6",
  "ch.qos.logback" % "logback-classic" % "1.2.3",
  "org.scalatest" %% "scalatest" % "3.0.5" % "test"
)
