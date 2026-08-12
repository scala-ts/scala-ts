organization := "io.github.scala-ts"

name := "sbt-plugin-test-simple"

version := "1.0-SNAPSHOT"

// 3.8.4 matches core_3 published with the sbt 2 plugin (shared _3 artifact)
crossScalaVersions := Seq("2.12.20", "2.13.18", "3.8.4")

enablePlugins(ScalatsGeneratorPlugin) // Required as disabled by default

// Distribute src/test/typescript as ts-test
Compile / compile :=  Def.uncached {
  val res = (Compile / compile).value
  val src = (Test / sourceDirectory).value / "typescript"
  val dest = (scalatsOnCompile / sourceManaged).value / "ts-test"

  sbt.io.IO.copyDirectory(src, dest, overwrite = true)

  res
}

TaskKey[Unit]("preserveGeneratedTypescript") := {
  import sbt.io.IO
  val logger = streams.value.log

  val tmpdir: File = sys.props.get("scala-ts.sbt-test-temp") match {
    case Some(path) => {
      val dir = new File(path)
      dir.mkdirs()
      dir
    }

    case _ => IO.createTemporaryDirectory
  }
  val destdir = tmpdir / name.value / "target"

  destdir.mkdirs()

  logger.info(s"Copying directory ${baseDirectory.value / "target"} to ${destdir} ...")

  IO.copyDirectory(baseDirectory.value / "target", destdir)
}
