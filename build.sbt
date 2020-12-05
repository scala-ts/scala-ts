import sbt.Keys._

name := "scala-ts"

organization in ThisBuild := "org.scala-ts"

lazy val core = project.in(file("core")).settings(
  name := "scala-ts-core",
  libraryDependencies ++= Seq(
    "org.scala-lang" % "scala-reflect" % scalaVersion.value,
    "org.scala-lang" % "scala-compiler" % scalaVersion.value,
    "org.slf4j" % "slf4j-api" % "1.7.30",
    "ch.qos.logback" % "logback-classic" % "1.1.7" % Test,
    "org.scalatest" %% "scalatest" % "3.2.3" % Test
  ),
  mainClass in (Compile, run) := Some("org.scalats.Main"),
  compile in Test := (compile in Test).dependsOn(
    packageBin in Compile/* make sure plugin.jar is available */).value
  /* TODO: (Re)move 
  ,scalacOptions in Test ++= {
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
  } */
)

lazy val `sbt-plugin` = project.in(file("sbt-plugin")).
  settings(
    name := "scala-ts-sbt",
    sbtPlugin := true
  ).dependsOn(core)

lazy val root = (project in file("."))
  .aggregate(core, `sbt-plugin`)
