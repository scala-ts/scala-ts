organization := "io.github.scala-ts"

name := "sbt-plugin-test-simple"

version := "1.0-SNAPSHOT"

crossScalaVersions := Seq("2.12.12", "2.13.6")

enablePlugins(TypeScriptGeneratorPlugin) // Required as disabled by default

// Distribute src/test/typescript as ts-test
Compile / compile := {
  val res = (Compile / compile).value
  val src = (sourceDirectory in Test).value / "typescript"
  val dest = (sourceManaged in scalatsOnCompile).value / "ts-test"

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

  logger.info(s"Copying directory ${target.value} to ${destdir} ...")

  IO.copyDirectory(target.value, destdir)
}
