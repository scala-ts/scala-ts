organization := "io.github.scala-ts"

name := "sbt-plugin-test-enumeratum"

version := "1.0-SNAPSHOT"

crossScalaVersions := Seq("2.12.14", "2.13.4")

enablePlugins(TypeScriptGeneratorPlugin) // Required as disabled by default

libraryDependencies ++= Seq(
  "com.beachape" %% "enumeratum" % "1.6.1")

scalatsUnionWithLiteral

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
