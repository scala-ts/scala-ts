inThisBuild(Seq(
  organization := "io.github.scala-ts",
  version := "1.0-SNAPSHOT",
  scalaVersion := "2.13.5"
))

val common = (project in file("common")).
  enablePlugins(TypeScriptGeneratorPlugin). // Required as disabled by default
  settings(
    // Distribute src/test/typescript as ts-test
    Compile / compile := {
      val res = (Compile / compile).value
      val src = (sourceDirectory in Test).value / "typescript"
      val dest = (sourceManaged in scalatsOnCompile).value / "ts-test"

      sbt.io.IO.copyDirectory(src, dest, overwrite = true)

      res
    }
  )

val api = (project in file("api")).dependsOn(common).
  enablePlugins(TypeScriptGeneratorPlugin). // Required as disabled by default
  settings(
    scalatsTypeExcludes := Set(".*\\.common\\..*"),
    // Distribute src/test/typescript as ts-test
    Compile / compile := {
      val res = (Compile / compile).value
      val src = (sourceDirectory in Test).value / "typescript"
      val dest = (sourceManaged in scalatsOnCompile).value / "ts-test"

      sbt.io.IO.copyDirectory(src, dest, overwrite = true)

      res
    }
  )

lazy val root = (project in file(".")).settings(
  name := "sbt-plugin-test-multi",
  publish := ({}),
  publishTo := None
).aggregate(common, api)
