organization := "io.github.scala-ts"

name := "sbt-plugin-test-single-file-printer"

version := "1.0-SNAPSHOT"

resolvers ++= Resolver.sonatypeOssRepos("snapshots")

scalaVersion := "2.13.14"

crossScalaVersions := Seq(scalaVersion.value, "3.4.3")

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

  logger.info(s"Copying directory ${target.value} to ${destdir} ...")

  IO.copyDirectory(target.value, destdir)
}
