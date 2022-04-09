inThisBuild(
  Seq(
    organization := "io.github.scala-ts",
    version := "1.0-SNAPSHOT",
    scalaVersion := "2.13.8"
  )
)

val common = (project in file("common"))
  .enablePlugins(TypeScriptGeneratorPlugin)
  . // Required as disabled by default
  settings(
    // Distribute src/test/typescript as ts-test
    Compile / compile := {
      val res = (Compile / compile).value
      val src = (Test / sourceDirectory).value / "typescript"
      val dest = (scalatsOnCompile / sourceManaged).value / "ts-test"

      sbt.io.IO.copyDirectory(src, dest, overwrite = true)

      res
    }
  )

val api = (project in file("api"))
  .dependsOn(common)
  .enablePlugins(TypeScriptGeneratorPlugin)
  . // Required as disabled by default
  settings(
    scalatsTypeExcludes := Set(".*\\.common\\..*"),
    // Distribute src/test/typescript as ts-test
    Compile / compile := {
      val res = (Compile / compile).value
      val src = (Test / sourceDirectory).value / "typescript"
      val dest = (scalatsOnCompile / sourceManaged).value / "ts-test"

      sbt.io.IO.copyDirectory(src, dest, overwrite = true)

      res
    }
  )

lazy val root = (project in file("."))
  .settings(
    name := "sbt-plugin-test-multi",
    publish := ({}),
    publishTo := None
  )
  .aggregate(common, api)

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

  IO.copyDirectory((common / target).value, destdir)
  IO.copyDirectory((api / target).value, destdir)
}
