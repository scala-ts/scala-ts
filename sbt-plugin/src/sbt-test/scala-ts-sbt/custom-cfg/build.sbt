organization := "io.github.scalats"

name := "sbt-plugin-test-custom-cfg"

version := "1.0-SNAPSHOT"

// Custom field naming
scalatsFieldNaming := classOf[scalats.CustomFieldNaming]

// Overwrite the directory the printer is initialized with
sourceManaged in scalatsOnCompile := {
  val dir = target.value / "_custom"
  dir.mkdirs()
  dir
}

// Custom printer
scalatsPrinter := classOf[scalats.CustomPrinter]

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
