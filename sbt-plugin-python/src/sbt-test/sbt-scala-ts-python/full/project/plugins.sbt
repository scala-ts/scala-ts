lazy val pluginVer = sys.props("scala-ts.version")

addSbtPlugin("io.github.scala-ts" %% "sbt-scala-ts" % pluginVer)

addSbtPlugin(
  "io.github.scala-ts" %% "sbt-scala-ts-python" % pluginVer changing ()
)
