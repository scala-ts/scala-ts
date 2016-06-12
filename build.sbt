import sbt.Keys._

lazy val xTask = TaskKey[String]("x")

lazy val root = (project in file(".")).
  settings(
    name := "scala-ts",
    version := "0.1.0",
    organization := "com.mpc",
    scalaVersion := "2.10.6",
    mainClass in (Compile, run) := Some("com.mpc.scalats.Main"),
    sbtPlugin := true,
    sbtVersion := "0.13.11",
    xTask := {
      "result"
    }
  )

libraryDependencies ++= Seq(
  "org.scala-lang" % "scala-reflect" % "2.10.6",
  "ch.qos.logback" % "logback-classic" % "1.1.7"
//  "com.typesafe.scala-logging" %% "scala-logging" % "3.4.0"
)