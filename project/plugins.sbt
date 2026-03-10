resolvers += Resolver.sonatypeRepo("staging")

addSbtPlugin("org.scalameta" % "sbt-scalafmt" % "2.5.6")

addSbtPlugin("ch.epfl.scala" % "sbt-scalafix" % "0.14.5")

addSbtPlugin("net.virtual-void" % "sbt-dependency-graph" % "0.10.0-RC1")

addSbtPlugin("com.github.sbt" % "sbt-dynver" % "5.1.1")

addSbtPlugin(("com.github.sbt" % "sbt-native-packager" % "1.9.10").
  exclude("org.scala-lang.modules", "*"))

val scalaTSVer = "0.8.1"

addSbtPlugin("io.github.scala-ts" % "sbt-scala-ts" % scalaTSVer)
