organization := "io.github.scala-ts"

name := "sbt-plugin-test-idtlt-full"

version := "1.0-SNAPSHOT"

enablePlugins(ScalatsIdtltPlugin) // Required as disabled by default

scalaVersion := "2.13.18"

crossScalaVersions := Seq("2.12.20", scalaVersion.value)

resolvers += "Sonatype Snapshots" at "https://central.sonatype.com/repository/maven-snapshots/"

// ---

// Distribute src/test/typescript as ts-test
Compile / compile := {
  val res = (Compile / compile).value
  val src = (Test / sourceDirectory).value / "typescript"
  val dest = (scalatsOnCompile / sourceManaged).value / "ts-test"

  sbt.io.IO.copyDirectory(src, dest, overwrite = true)

  res
}

TaskKey[Unit]("preserveGeneratedTypescript") := {
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
