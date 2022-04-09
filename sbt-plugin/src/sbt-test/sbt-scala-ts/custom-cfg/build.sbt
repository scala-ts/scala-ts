organization := "io.github.scala-ts"

name := "sbt-plugin-test-custom-cfg"

version := "1.0-SNAPSHOT"

enablePlugins(TypeScriptGeneratorPlugin) // Required as disabled by default

// Custom option transpiling
scalatsOptionToNullable := true

// TODO: cross example with custom class in separate module

// Custom type naming (defined in `project/CustomTypeNaming.scala`)
scalatsTypeScriptTypeNaming := classOf[scalats.CustomTypeNaming]

// Custom field naming (defined in `project/CustomFieldMapper.scala`)
scalatsTypeScriptFieldMapper := classOf[scalats.CustomFieldMapper]

// Overwrite the directory the printer is initialized with
scalatsOnCompile / sourceManaged := {
  val dir = target.value / "_custom"
  dir.mkdirs()
  dir
}

// Custom printer (defined in `project/CustomPrinter.scala`)
scalatsPrinter := scalatsPrinterForClass[scalats.CustomPrinter]()

scalatsPrinterPrelude := scalatsPrinterInMemoryPrelude(
  "import { Option } from 'space-monad'",
  "// could be useful to import common types"
)

// Custom declaration mapper (before type mapper)
scalatsTypeScriptDeclarationMappers := Seq(
  scalatsEnumerationAsEnum,
  scalatsValueClassAsTagged,
  classOf[scalats.CustomDeclarationMapper]
  // defined in `project/CustomDeclarationMapper.scala`
)

// Custom type mapper
scalatsTypeScriptTypeMappers := Seq(
  scalatsNullableAsOption, // Also scalatsDateAsString, scalatsNumberAsString
  classOf[scalats.CustomTypeMapper]
  // defined in `project/CustomTypeMapper.scala`
)

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
