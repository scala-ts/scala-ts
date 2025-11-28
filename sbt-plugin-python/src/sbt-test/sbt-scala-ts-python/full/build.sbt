organization := "io.github.scala-ts"

name := "sbt-plugin-test-python-full"

version := "1.0-SNAPSHOT"

enablePlugins(ScalatsPythonPlugin) // Required as disabled by default

scalaVersion := "2.13.18"

crossScalaVersions := Seq("2.12.20", scalaVersion.value)

scalatsPythonBaseModule := Some("generated")

scalatsOnCompile / sourceManaged := {
  val dir = target.value / "scala-ts" / "generated"
  dir.mkdirs()
  dir
}

// ---

// Distribute src/test/typescript as ts-test
Compile / compile := {
  val res = (Compile / compile).value
  val src = (Test / sourceDirectory).value / "typescript"
  val dest = (scalatsOnCompile / sourceManaged).value / "ts-test"

  sbt.io.IO.copyDirectory(src, dest, overwrite = true)

  res
}

TaskKey[Unit]("preserveGeneratedPython") := {
  import sbt.io.IO
  val logger = streams.value.log

  sys.props.get("scala-ts.sbt-test-temp") match {
    case Some(path) => {
      val tmpdir = new File(path)
      tmpdir.mkdirs()

      val destdir = tmpdir / name.value / "target"
      destdir.mkdirs()

      logger.info(s"Copying directory ${target.value} to ${destdir} ...")

      IO.copyDirectory(target.value, destdir)
    }

    case _ => ()
  }
}
