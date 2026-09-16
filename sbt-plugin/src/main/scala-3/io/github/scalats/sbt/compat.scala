package io.github.scalats.sbt

import sbt._
import sbt.Keys._
import xsbti.FileConverter

private[sbt] object ClasspathsCompat {

  def managedJars(
      config: Configuration,
      types: Set[String],
      report: UpdateReport
    )(using
      conv: FileConverter
    ): Classpath =
    Classpaths.managedJars(config, types, report, conv)

}

private[sbt] object Compat {
  import sbt.util.CacheImplicits.given

  import ScalatsGeneratorPlugin.autoImport.scalatsOnCompile

  def settings: Seq[Def.Setting[_]] = Seq(
    Compile / compileIncremental := Def.cachedTask {
      val res = (Compile / compileIncremental).value
      val dir = (scalatsOnCompile / sourceManaged).value

      val generatedPath = dir.toPath

      val conv = fileConverter.value

      Def.declareOutputDirectory(conv.toVirtualFile(generatedPath))

      res
    }.value
  )
}
