import sbt.Keys._

name := "scala-ts"

organization in ThisBuild := "io.github.scala-ts"

lazy val core = project.in(file("core")).settings(
  name := "scala-ts-core",
  crossScalaVersions := Seq("2.11.12", scalaVersion.value, "2.13.3"),
  unmanagedSourceDirectories in Compile += {
    val base = (sourceDirectory in Compile).value

    CrossVersion.partialVersion(scalaVersion.value) match {
      case Some((2, n)) if n >= 12 => base / "scala-2.12+"
      case _                       => base / "scala-2.12-"
    }
  },
  libraryDependencies ++= Seq(
    "org.scala-lang" % "scala-reflect" % scalaVersion.value,
    "org.scala-lang" % "scala-compiler" % scalaVersion.value,
    "org.slf4j" % "slf4j-api" % "1.7.30",
    "ch.qos.logback" % "logback-classic" % "1.1.7" % Test,
    "org.scalatest" %% "scalatest" % "3.2.3" % Test
  ),
  libraryDependencies ++= {
    if (scalaBinaryVersion.value == "2.13") {
      Seq("org.scala-lang.modules" %% "scala-xml" % "1.3.0")
    } else {
      Seq.empty
    }
  },
  mainClass in (Compile, run) := Some("io.github.scalats.Main"),
  compile in Test := (compile in Test).dependsOn(
    packageBin in Compile/* make sure plugin.jar is available */).value
)

lazy val `sbt-plugin` = project.in(file("sbt-plugin")).
  enablePlugins(SbtPlugin).
  settings(
    name := "scala-ts-sbt",
    crossScalaVersions := Seq(scalaVersion.value),
    pluginCrossBuild / sbtVersion := "1.3.13",
    sbtPlugin := true,
    scriptedLaunchOpts ++= Seq(
      "-Xmx1024M",
      s"-Dscala-ts.version=${version.value}",
      s"-Dscala-ts.sbt-test-temp=/tmp/${name.value}"
    ),
    // TODO: core / publishLocal on update?
    scriptedBufferLog := false,
    sourceGenerators in Compile += Def.task {
      val groupId = organization.value
      val coreArtifactId = (core / name).value
      val ver = version.value
      val dir = (sourceManaged in Compile).value
      val outdir = dir / "org" / "scalats" / "sbt"
      val f = outdir / "Manifest.scala"

      outdir.mkdirs()

      Seq(IO.writer[File](f, "", IO.defaultCharset, false) { w =>
        w.append(s"""package io.github.scalats.sbt

object Manifest {
  val groupId = "$groupId"
  val coreArtifactId = "$coreArtifactId"
  val version = "$ver"
}""")

        f
      })
    }.taskValue
  ).dependsOn(core)

lazy val root = (project in file("."))
  .settings(
    publish := ({}),
    publishTo := None,
  )
  .aggregate(core, `sbt-plugin`)
