lazy val pluginVer = sys.props("scala-ts.version")

libraryDependencies += "io.github.scala-ts" %% "scala-ts-core" % pluginVer

addSbtPlugin("io.github.scala-ts" % "sbt-scala-ts" % pluginVer)
