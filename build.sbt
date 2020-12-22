import sbt.Keys._

name := "scala-ts"

organization in ThisBuild := "io.github.scala-ts"

lazy val shaded = project.in(file("shaded")).settings(
  name := "scala-ts-shaded",
  crossPaths := false,
  autoScalaLibrary := false,
  libraryDependencies += "com.typesafe" % "config" % "1.4.1",
  assemblyShadeRules in assembly := Seq(
    ShadeRule.rename(
      "com.typesafe.config.**" -> "io.github.scalats.tsconfig.@1").inAll
  ),
  publish := ({}),
  publishTo := None,
)

lazy val core = project.in(file("core")).settings(
  name := "scala-ts-core",
  crossScalaVersions := Seq("2.11.12", scalaVersion.value, "2.13.4"),
  unmanagedJars in Compile += {
    val jarName = (shaded / assembly / assemblyJarName).value

    (shaded / target).value / jarName
  },
  unmanagedSourceDirectories in Compile += {
    val base = (sourceDirectory in Compile).value

    CrossVersion.partialVersion(scalaVersion.value) match {
      case Some((2, n)) if n >= 12 => base / "scala-2.12+"
      case _                       => base / "scala-2.12-"
    }
  },
  compile in Compile := (compile in Compile).dependsOn(shaded / assembly).value,
  libraryDependencies ++= {
    val specsVer = "4.10.5"

    Seq(
      "org.scala-lang" % "scala-reflect" % scalaVersion.value,
      "org.scala-lang" % "scala-compiler" % scalaVersion.value,
      "org.slf4j" % "slf4j-api" % "1.7.30",
      "ch.qos.logback" % "logback-classic" % "1.1.7" % Test,
      "org.specs2" %% "specs2-core" % specsVer % Test,
      "org.specs2" %% "specs2-junit" % specsVer % Test)
  },
  assemblyExcludedJars in assembly := {
    (fullClasspath in assembly).value.filterNot {
      _.data.getName startsWith "scala-ts-shaded"
    }
  },
  pomPostProcess := XmlUtil.transformPomDependencies { dep =>
    (dep \ "groupId").headOption.map(_.text) match {
      case Some(
        "com.sksamuel.scapegoat" | // plugin there (compile time only)
          "com.github.ghik" // plugin there (compile time only)
      ) =>
        None

      case Some("io.github.scala-ts") =>
        Some(dep).filter { _ =>
          (dep \ "artifactId").headOption.
            exists(_ startsWith "scala-ts-shaded")
        }

      case _ =>
        Some(dep)
    }
  },
  packageBin in Compile := crossTarget.value / (
    assemblyJarName in assembly).value,
  makePom := makePom.dependsOn(assembly).value,
  mainClass in assembly := Some("io.github.scalats.Main"),
  mainClass in (Compile, run) := (mainClass in assembly).value
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
    compile in Compile := (compile in Compile).dependsOn(
      core / publishLocal).value,
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
  .aggregate(shaded, core, `sbt-plugin`)
