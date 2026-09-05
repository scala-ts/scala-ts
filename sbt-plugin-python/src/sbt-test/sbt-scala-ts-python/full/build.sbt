organization := "io.github.scala-ts"

name := "sbt-plugin-test-python-full"

version := "1.0-SNAPSHOT"

enablePlugins(ScalatsPythonPlugin) // Required as disabled by default

scalaVersion := "3.9.0"

scalatsPythonBaseModule := Some("generated")

scalatsOnCompile / sourceManaged := {
  val dir = baseDirectory.value / "target" / "scala-ts" / "generated"
  dir.mkdirs()
  dir
}

/*
// ---

// Distribute src/test/python as ts-test
Compile / compile :=  Def.uncached {
  val res = (Compile / compile).value
  val src = (Test / sourceDirectory).value / "python"
  val dest = baseDirectory.value / "target" / "ts-test"

  sbt.io.IO.copyDirectory(src, dest, overwrite = true)

  //sbt.io.IO.move(dest, (scalatsOnCompile / sourceManaged).value / "ts-test")

  res
}
 */

TaskKey[Unit]("preserveGeneratedPython") := {
  import sbt.io.IO
  val logger = streams.value.log

  sys.props.get("scala-ts.sbt-test-temp") match {
    case Some(path) => {
      val tmpdir = new File(path)
      tmpdir.mkdirs()

      val destdir = tmpdir / name.value / "target"
      destdir.mkdirs()

      logger.info(
        s"Copying directory ${baseDirectory.value / "target"} to ${destdir} ..."
      )

      IO.copyDirectory(baseDirectory.value / "target", destdir)
    }

    case _ => ()
  }
}
