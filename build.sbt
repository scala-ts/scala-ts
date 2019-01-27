import sbt.Keys._

name in ThisBuild := "scala-ts"

organization in ThisBuild := "org.scala-ts"

val commonSettings = Publish.settings

lazy val core = project.in(file("core")).settings(Seq(
  name := "scala-ts-core",
  libraryDependencies ++= Seq(
    "org.scala-lang" % "scala-reflect" % scalaVersion.value,
    "org.scala-lang" % "scala-compiler" % scalaVersion.value,
    "ch.qos.logback" % "logback-classic" % "1.1.7",
    "org.scalatest" %% "scalatest" % "3.0.8" % Test
  ),
  mainClass in (Compile, run) := Some("org.scalats.Main"),
  compile in Test := (compile in Test).dependsOn(
    packageBin in Compile/* make sure plugin.jar is available */).value,
  scalacOptions in Test ++= {
    val v = version.value
    val sv = scalaVersion.value
    val b = (baseDirectory in Compile).value
    val n = (name in Compile).value
    val msv = scalaBinaryVersion.value

    val td = b / "target" / s"scala-$msv"
    val j = td / s"${n}_$msv-$v.jar"

    val testCfg = (resourceDirectory in Test).value / "plugin-conf.xml"

    Seq(
      s"-Xplugin:${j.getAbsolutePath}",
      "-P:scalats:debug",
      s"-P:scalats:configuration=${testCfg.getAbsolutePath}")
  }
) ++ commonSettings)

lazy val `sbt-plugin` = project.in(file("sbt-plugin")).
  settings(
    name := "scala-ts-sbt",
    sbtPlugin := true
  ).dependsOn(core)

lazy val root = (project in file("."))
  .aggregate(core, `sbt-plugin`)
  .settings(commonSettings)
