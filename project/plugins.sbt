resolvers += Resolver.sonatypeRepo("staging")

addSbtPlugin("org.scalameta" % "sbt-scalafmt" % "2.6.2")

addSbtPlugin("ch.epfl.scala" % "sbt-scalafix" % "0.14.7")

addSbtPlugin("net.virtual-void" % "sbt-dependency-graph" % "0.10.0-RC1")

addSbtPlugin("com.github.sbt" % "sbt-dynver" % "5.1.1")

addSbtPlugin(
  ("com.github.sbt" % "sbt-native-packager" % "1.9.10")
    .exclude("org.scala-lang.modules", "*")
)

val scalaTSVer = "0.8.2"

addSbtPlugin("io.github.scala-ts" % "sbt-scala-ts" % scalaTSVer)

addSbtPlugin(
  ("io.github.scala-ts" % "sbt-scala-ts-idtlt" % scalaTSVer).changing()
)
