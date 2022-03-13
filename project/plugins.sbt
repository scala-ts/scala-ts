resolvers += Resolver.sonatypeRepo("staging")

addSbtPlugin("org.scalameta" % "sbt-scalafmt" % "2.4.3")

addSbtPlugin("ch.epfl.scala" % "sbt-scalafix" % "0.9.34")

addSbtPlugin("net.virtual-void" % "sbt-dependency-graph" % "0.10.0-RC1")

addSbtPlugin("com.sksamuel.scapegoat" %% "sbt-scapegoat" % "1.1.1")

addSbtPlugin("com.dwijnand" % "sbt-dynver" % "4.1.1")

addSbtPlugin("com.typesafe.sbt" % "sbt-native-packager" % "1.3.1")

val scalaTSVer = "0.5.10"

addSbtPlugin("io.github.scala-ts" % "sbt-scala-ts" % scalaTSVer)

addSbtPlugin(
  "io.github.scala-ts" % "sbt-scala-ts-idtlt" % scalaTSVer changing ()
)
