import sbt.Keys._
import sbtcompat.PluginCompat._

name := "scala-ts"

ThisBuild / organization := "io.github.scala-ts"

lazy val shaded = project
  .in(file("shaded"))
  .settings(
    name := "scala-ts-shaded",
    crossPaths := false,
    autoScalaLibrary := false,
    libraryDependencies += "com.typesafe" % "config" % "1.4.9",
    assembly / assemblyShadeRules := Seq(
      ShadeRule
        .rename("com.typesafe.config.**" -> "io.github.scalats.tsconfig.@1")
        .inAll
    ),
    exportJars := false,
    publish := ({}),
    publishTo := None
  )

val scala213Version = "2.13.18"
// LTS for libraries / user compiler-plugin artifacts
val scala3Lts = "3.4.3"
// sbt 2.0.6 is itself built with Scala 3.8.4 (TASTy 28.8); the sbt-plugin
// must use a matching compiler to read sbt's classpath (3.4.x cannot).
val scala3ForSbt2 = "3.8.4"
val sbt1Version = "1.5.8"
val sbt2Version = "2.0.6"
val scriptedSbt1Version = "1.12.15"

ThisBuild / libraryDependencySchemes ++= {
  if (scalaBinaryVersion.value == "2.12") {
    Seq("org.scala-lang.modules" %% "scala-xml" % VersionScheme.Always)
  } else {
    Seq.empty
  }
}

val fullCrossScalaVersions = Def.setting {
  Seq(
    scalaVersion.value,
    scala213Version,
    scala3Lts,
    // Needed so sbt-plugin can dependsOn(core) when cross-built for sbt 2
    scala3ForSbt2
  )
}

// Dual-publish sbt plugins: sbt 1.x (Scala 2.12) + sbt 2.x (Scala 3.8.x)
lazy val sbtPluginCrossSettings = Seq[Setting[?]](
  addSbtPlugin("com.github.sbt" % "sbt2-compat" % "0.2.0"),
  crossScalaVersions := Seq(
    (LocalRootProject / scalaVersion).value,
    scala3ForSbt2
  ),
  pluginCrossBuild / sbtVersion := {
    (pluginCrossBuild / scalaBinaryVersion).value match {
      case "3" => sbt2Version
      case _   => sbt1Version
    }
  },
  scriptedSbt := {
    scalaBinaryVersion.value match {
      case "3" => sbt2Version
      case _   => scriptedSbt1Version
    }
  },
  scalacOptions ++= {
    if (scalaBinaryVersion.value == "3") {
      // Keep shared plugin sources compiling with Setting[_] on Scala 3
      Seq("-Wconf:msg=.*deprecated for wildcard arguments of types.*:s")
    } else {
      Seq("-Xsource:3")
    }
  }
)

val copyAssemblyJar: Def.Initialize[Task[Unit]] = Def.task {
  import _root_.sbtcompat.PluginCompat.toFile

  implicit val conv: xsbti.FileConverter = fileConverter.value

  IO.copyFile(toFile(assembly.value), toFile((Compile / packageBin).value))
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
    Compile / unmanagedJars += Def.uncached((shaded / assembly).value),
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
      val specsVer = "4.23.0"

      Seq(
        "org.slf4j" % "slf4j-api" % "1.7.36",
        "ch.qos.logback" % "logback-classic" % "1.6.3"
      ) ++ Seq("core", "junit").map(n =>
        ("org.specs2" %% s"specs2-${n}" % specsVer)
          .cross(CrossVersion.for3Use2_13) % Test
      )
    },
    dependencyOverrides ++= {
      scalaBinaryVersion.value match {
        case "2.13" =>
          Seq("org.scala-lang.modules" %% "scala-xml" % "2.4.0")
        case _ =>
          Seq.empty
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
    assembly / assemblyExcludedJars := {
      implicit val conv: xsbti.FileConverter = fileConverter.value

      val selfJar = toFile((Compile / packageBin).value).getName

      (assembly / fullClasspath).value.filterNot { f =>
        val nme = toFile(f).getName

        nme == selfJar || nme.startsWith("scala-ts-shaded")
      }
    },
    makePom := makePom.dependsOn(copyAssemblyJar).value,
    assembly / mainClass := Some("io.github.scalats.Main"),
    Compile / run / mainClass := (assembly / mainClass).value
  )

lazy val `sbt-plugin` = project
  .in(file("sbt-plugin"))
  .enablePlugins(SbtPlugin)
  .settings(sbtPluginCrossSettings)
  .settings(
    name := "sbt-scala-ts",
    sbtPlugin := true,
    scriptedLaunchOpts ++= Seq(
      "-Xmx1024M",
      s"-Dscala-ts.version=${version.value}",
      s"-Dscala-ts.sbt-test-temp=/tmp/${name.value}"
    ),
    Compile / unmanagedJars += Def.uncached((shaded / assembly).value),
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
    Compile / unmanagedJars += Def.uncached((shaded / assembly).value),
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
  .settings(sbtPluginCrossSettings)
  .settings(
    name := "sbt-scala-ts-idtlt",
    sbtPlugin := true,
    scriptedLaunchOpts := (`sbt-plugin` / scriptedLaunchOpts).value,
    Compile / compile := Def.uncached {
      (Compile / compile).dependsOn(`sbt-plugin` / Compile / compile).value
    },
    Compile / unmanagedJars := Def.uncached {
      implicit val conv: xsbti.FileConverter = fileConverter.value

      toAttributedFiles(
        Seq(
          toFile((`sbt-plugin` / Compile / packageBin).value),
          (shaded / target).value / (shaded / assembly / assemblyJarName).value
        )
      )
    },
    scripted := scripted
      .dependsOn(core / publishLocal)
      .dependsOn(idtlt / publishLocal)
      .dependsOn(`sbt-plugin` / publishLocal)
      .dependsOn(publishLocal)
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
    Compile / unmanagedJars += Def.uncached((shaded / assembly).value),
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
  .settings(sbtPluginCrossSettings)
  .settings(
    name := "sbt-scala-ts-python",
    sbtPlugin := true,
    scriptedLaunchOpts := (`sbt-plugin` / scriptedLaunchOpts).value,
    Compile / compile := Def.uncached {
      (Compile / compile).dependsOn(`sbt-plugin` / Compile / compile).value
    },
    Compile / unmanagedJars := Def.uncached {
      implicit val conv: xsbti.FileConverter = fileConverter.value

      toAttributedFiles(
        Seq(
          toFile((`sbt-plugin` / Compile / packageBin).value),
          (shaded / target).value / (shaded / assembly / assemblyJarName).value
        )
      )
    },
    scripted := scripted
      .dependsOn(core / publishLocal)
      .dependsOn(python / publishLocal)
      .dependsOn(`sbt-plugin` / publishLocal)
      .dependsOn(publishLocal)
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
