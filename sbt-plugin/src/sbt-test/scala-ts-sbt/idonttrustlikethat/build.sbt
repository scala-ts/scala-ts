organization := "io.github.scala-ts"

name := "sbt-plugin-test-idonttrustlikethat"

version := "1.0-SNAPSHOT"

enablePlugins(TypeScriptGeneratorPlugin) // Required as disabled by default

// Prelude required by the declaration mapper
scalatsPrinterPrelude := {
  val imports = "import * as idtlt from 'idonttrustlikethat';"

  scalatsPrinterPrelude.value.map {
    case Left(prelude) =>
      Left(prelude :+ imports)

    case prelude @ Right(url) => {
      println(s"Scala-TS prelude is set to ${url}; Make sure it include the required imports:\r\n\t${imports}")

      prelude
    }
  }.orElse {
    scalatsPrinterInMemoryPrelude(imports)
  }
}

// Custom type mapper
scalatsTypeScriptTypeMappers := Seq(
  classOf[scalats.CustomTypeMapper]
)

// Custom declaration mapper (before type mapper)
scalatsTypeScriptDeclarationMappers := Seq(
  classOf[scalats.CustomDeclarationMapper] // TODO: Refactor as scala-ts module
)

scalacOptions in Compile ++= Seq(
  "-P:scalats:sys.scala-ts.printer.import-pattern=* as ns%1$s")

scalatsTypeScriptImportResolvers ++= Seq(
  scalatsUnionWithLiteralSingletonImportResolvers)

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
