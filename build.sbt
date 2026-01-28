import sbt.Keys._

name := "scala-ts"

ThisBuild / organization := "io.github.scala-ts"

lazy val shaded = project
  .in(file("shaded"))
  .settings(
    name := "scala-ts-shaded",
    crossPaths := false,
    autoScalaLibrary := false,
    libraryDependencies += "com.typesafe" % "config" % "1.4.5",
    assembly / assemblyShadeRules := Seq(
      ShadeRule
        .rename("com.typesafe.config.**" -> "io.github.scalats.tsconfig.@1")
        .inAll
    ),
    publish := ({}),
    publishTo := None
  )

val scala213Version = "2.13.18"

val fullCrossScalaVersions = Def.setting {
  Seq(
    "2.11.12",
    scalaVersion.value,
    scala213Version,
    "3.4.2"
  )
}

lazy val core = project
  .in(file("core"))
  .settings(
    name := "scala-ts-core",
    crossScalaVersions := fullCrossScalaVersions.value,
    scalacOptions ++= {
      if (scalaBinaryVersion.value == "3") {
        Seq("-Wconf:cat=deprecation&msg=.*JavaConverters.*:s")
      } else {
        Seq.empty
      }
    },
    Compile / unmanagedJars += (shaded / assembly).value,
    Compile / unmanagedSourceDirectories += {
      val base = (Compile / sourceDirectory).value

      CrossVersion.partialVersion(scalaVersion.value) match {
        case Some((2, n)) if n < 12 => base / "scala-2.12-"
        case _                      => base / "scala-2.12+"
      }
    },
    libraryDependencies ++= {
      val v = scalaVersion.value

      if (scalaBinaryVersion.value == "3") {
        Seq("org.scala-lang" %% "scala3-compiler" % v)
      } else {
        Seq(
          "org.scala-lang" % "scala-reflect" % v,
          "org.scala-lang" % "scala-compiler" % v
        )
      }
    },
    libraryDependencies ++= {
      val specsVer = "4.10.6"

      Seq(
        "org.slf4j" % "slf4j-api" % "1.7.36",
        "ch.qos.logback" % "logback-classic" % "1.5.26"
      ) ++ Seq("core", "junit").map(n =>
        ("org.specs2" %% s"specs2-${n}" % specsVer)
          .cross(CrossVersion.for3Use2_13) % Test
      )
    },
    assembly / assemblyExcludedJars := {
      (assembly / fullClasspath).value.filterNot {
        _.data.getName startsWith "scala-ts-shaded"
      }
    },
    Test / classLoaderLayeringStrategy := ClassLoaderLayeringStrategy.Flat,
    pomPostProcess := XmlUtil.transformPomDependencies { dep =>
      (dep \ "groupId").headOption.map(_.text) match {
        case Some(
              "com.github.ghik" // plugin there (compile time only)
            ) =>
          None

        case Some("io.github.scala-ts") =>
          Some(dep).filter { _ =>
            (dep \ "artifactId").headOption
              .exists(_ startsWith "scala-ts-shaded")
          }

        case _ =>
          Some(dep)
      }
    },
    Compile / packageBin := crossTarget.value / (assembly / assemblyJarName).value,
    makePom := makePom.dependsOn(assembly).value,
    assembly / mainClass := Some("io.github.scalats.Main"),
    Compile / run / mainClass := (assembly / mainClass).value
  )

lazy val `sbt-plugin` = project
  .in(file("sbt-plugin"))
  .enablePlugins(SbtPlugin)
  .settings(
    name := "sbt-scala-ts",
    crossScalaVersions := Seq(scalaVersion.value),
    pluginCrossBuild / sbtVersion := "1.3.13",
    sbtPlugin := true,
    scriptedLaunchOpts ++= Seq(
      "-Xmx1024M",
      s"-Dscala-ts.version=${version.value}",
      s"-Dscala-ts.sbt-test-temp=/tmp/${name.value}"
    ),
    Compile / unmanagedJars += {
      val jarName = (shaded / assembly / assemblyJarName).value

      (shaded / target).value / jarName
    },
    scripted := scripted.dependsOn(core / publishLocal).evaluated,
    scriptedBufferLog := false,
    Compile / sourceGenerators += Def.task {
      val groupId = organization.value
      val coreArtifactId = (core / name).value
      val ver = version.value
      val dir = (Compile / sourceManaged).value
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
  )
  .dependsOn(core)

lazy val idtlt = project
  .in(file("idtlt"))
  .settings(
    name := "scala-ts-idtlt",
    crossScalaVersions := fullCrossScalaVersions.value,
    Compile / unmanagedJars += (shaded / assembly).value,
    Test / classLoaderLayeringStrategy := ClassLoaderLayeringStrategy.Flat,
    pomPostProcess := XmlUtil.transformPomDependencies { dep =>
      (dep \ "groupId").headOption.map(_.text) match {
        case Some(
              "com.github.ghik" // plugin there (compile time only)
            ) =>
          None

        case Some("io.github.scala-ts") =>
          Some(dep).filter { _ =>
            (dep \ "artifactId").headOption
              .exists(_ startsWith "scala-ts-shaded")
          }

        case _ =>
          Some(dep)
      }
    }
  )
  .dependsOn(core % "compile->compile;test->test")

lazy val `sbt-plugin-idtlt` = project
  .in(file("sbt-plugin-idtlt"))
  .enablePlugins(SbtPlugin)
  .settings(
    name := "sbt-scala-ts-idtlt",
    crossScalaVersions := Seq(scalaVersion.value),
    pluginCrossBuild / sbtVersion := (`sbt-plugin` / pluginCrossBuild / sbtVersion).value,
    sbtPlugin := true,
    scriptedLaunchOpts ++= (`sbt-plugin` / scriptedLaunchOpts).value,
    Compile / compile := (Compile / compile)
      .dependsOn(`sbt-plugin` / Compile / compile)
      .value,
    Compile / unmanagedJars ++= {
      val jarName = (shaded / assembly / assemblyJarName).value

      Seq(
        (`sbt-plugin` / Compile / packageBin).value,
        (shaded / target).value / jarName
      )
    },
    scripted := scripted
      .dependsOn(core / publishLocal)
      .dependsOn(idtlt / publishLocal)
      .dependsOn(`sbt-plugin` / publishLocal)
      .evaluated,
    Compile / sourceGenerators += Def.task {
      val groupId = organization.value
      val coreArtifactId = (core / name).value
      val ver = version.value
      val dir = (Compile / sourceManaged).value
      val outdir = dir / "org" / "scalats" / "sbt" / "idtlt"
      val f = outdir / "Manifest.scala"

      outdir.mkdirs()

      Seq(IO.writer[File](f, "", IO.defaultCharset, false) { w =>
        w.append(s"""package io.github.scalats.sbt.idtlt

object Manifest {
  val groupId = "$groupId"
  val coreArtifactId = "$coreArtifactId"
  val version = "$ver"
}""")

        f
      })
    }.taskValue
  )
  .dependsOn(idtlt)

lazy val python = project
  .in(file("python"))
  .settings(
    name := "scala-ts-python",
    crossScalaVersions := fullCrossScalaVersions.value,
    Compile / unmanagedJars += (shaded / assembly).value,
    Test / classLoaderLayeringStrategy := ClassLoaderLayeringStrategy.Flat,
    pomPostProcess := XmlUtil.transformPomDependencies { dep =>
      (dep \ "groupId").headOption.map(_.text) match {
        case Some(
              "com.github.ghik" // plugin there (compile time only)
            ) =>
          None

        case Some("io.github.scala-ts") =>
          Some(dep).filter { _ =>
            (dep \ "artifactId").headOption
              .exists(_ startsWith "scala-ts-shaded")
          }

        case _ =>
          Some(dep)
      }
    }
  )
  .dependsOn(core % "compile->compile;test->test")

lazy val `sbt-plugin-python` = project
  .in(file("sbt-plugin-python"))
  .enablePlugins(SbtPlugin)
  .settings(
    name := "sbt-scala-ts-python",
    crossScalaVersions := Seq(scalaVersion.value),
    pluginCrossBuild / sbtVersion := (`sbt-plugin` / pluginCrossBuild / sbtVersion).value,
    sbtPlugin := true,
    scriptedLaunchOpts ++= (`sbt-plugin` / scriptedLaunchOpts).value,
    Compile / compile := (Compile / compile)
      .dependsOn(`sbt-plugin` / Compile / compile)
      .value,
    Compile / unmanagedJars ++= {
      val jarName = (shaded / assembly / assemblyJarName).value

      Seq(
        (`sbt-plugin` / Compile / packageBin).value,
        (shaded / target).value / jarName
      )
    },
    scripted := scripted
      .dependsOn(core / publishLocal)
      .dependsOn(python / publishLocal)
      .dependsOn(`sbt-plugin` / publishLocal)
      .evaluated,
    Compile / sourceGenerators += Def.task {
      val groupId = organization.value
      val coreArtifactId = (core / name).value
      val ver = version.value
      val dir = (Compile / sourceManaged).value
      val outdir = dir / "org" / "scalats" / "sbt" / "python"
      val f = outdir / "Manifest.scala"

      outdir.mkdirs()

      Seq(IO.writer[File](f, "", IO.defaultCharset, false) { w =>
        w.append(s"""package io.github.scalats.sbt.python

object Manifest {
  val groupId = "$groupId"
  val coreArtifactId = "$coreArtifactId"
  val version = "$ver"
}""")

        f
      })
    }.taskValue
  )
  .dependsOn(python)

lazy val root = (project in file("."))
  .settings(
    publish := ({}),
    publishTo := None
  )
  .aggregate(
    shaded,
    core,
    `sbt-plugin`,
    idtlt,
    `sbt-plugin-idtlt`,
    python,
    `sbt-plugin-python`
  )
