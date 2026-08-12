package io.github.scalats.sbt

import sbt._
import sbt.Keys.Classpath

import xsbti.FileConverter

private[sbt] object ClasspathsCompat {

  def managedJars(
      config: Configuration,
      types: Set[String],
      report: UpdateReport
    )(implicit
      conv: FileConverter
    ): Classpath = {
    // `conv` is part of the shared signature with the sbt 2 overload.
    if (conv eq null) ()
    Classpaths.managedJars(config, types, report)
  }
}
