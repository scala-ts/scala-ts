resolvers += Resolver.sonatypeRepo("staging")

addSbtPlugin("org.scalameta" % "sbt-scalafmt" % "2.4.6")

addSbtPlugin("ch.epfl.scala" % "sbt-scalafix" % "0.10.1")

addSbtPlugin("net.virtual-void" % "sbt-dependency-graph" % "0.10.0-RC1")

addSbtPlugin("com.dwijnand" % "sbt-dynver" % "4.1.1")

addSbtPlugin(("com.github.sbt" % "sbt-native-packager" % "1.9.10").
  exclude("org.scala-lang.modules", "*"))

val scalaTSVer = "0.5.14"

addSbtPlugin("io.github.scala-ts" % "sbt-scala-ts" % scalaTSVer)
