lazy val pluginVer = sys.props("scala-ts.version")

resolvers += Resolver.sonatypeRepo("staging")

resolvers += "Sonatype Snapshots" at "https://central.sonatype.com/repository/maven-snapshots/"

addSbtPlugin("io.github.scala-ts" %% "sbt-scala-ts" % pluginVer)

addSbtPlugin(
  "io.github.scala-ts" %% "sbt-scala-ts-idtlt" % pluginVer changing ()
)
