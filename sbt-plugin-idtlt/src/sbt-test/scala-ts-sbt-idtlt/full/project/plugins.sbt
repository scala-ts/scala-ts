lazy val pluginVer = sys.props("scala-ts.version")

addSbtPlugin("io.github.scala-ts" %% "scala-ts-sbt" % pluginVer)

addSbtPlugin("io.github.scala-ts" %% "scala-ts-sbt-idtlt" % pluginVer)
