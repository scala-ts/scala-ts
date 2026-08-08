package io.github.scalats.sbt

import sbt._
import sbt.Keys.Classpath
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
