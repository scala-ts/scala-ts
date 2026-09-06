organization := "io.github.scala-ts"

name := "sbt-plugin-test-single-file-printer"

version := "1.0-SNAPSHOT"

scalaVersion := "3.9.0"

// 3.8.4 matches core_3 published with the sbt 2 plugin (shared _3 artifact)
crossScalaVersions := Seq(scalaVersion.value, "3.8.4")

enablePlugins(ScalatsGeneratorPlugin) // Required as disabled by default

scalatsPrinter := scalatsSingleFilePrinter("generated.ts")

scalatsPrinterPrelude := scalatsPrinterUrlPrelude(
  (baseDirectory.value / "project" / "prelude.ts").toURI.toURL
)

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

  logger.info(
    s"Copying directory ${baseDirectory.value / "target"} to ${destdir} ..."
  )

  IO.copyDirectory(baseDirectory.value / "target", destdir)
}
