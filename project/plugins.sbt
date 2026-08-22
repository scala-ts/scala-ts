resolvers ++= Seq(
  "Tatami Releases" at "https://raw.github.com/cchantep/tatami/master/releases"
)

addDependencyTreePlugin

addSbtPlugin("com.github.sbt" % "sbt2-compat" % "0.2.0")

addSbtPlugin("org.scalameta" % "sbt-scalafmt" % "2.6.2")

addSbtPlugin("com.eed3si9n" % "sbt-assembly" % "2.4.2")

addSbtPlugin("ch.epfl.scala" % "sbt-scalafix" % "0.14.7")

addSbtPlugin("com.github.sbt" % "sbt-dynver" % "5.1.1")

addSbtPlugin("com.github.sbt" % "sbt-pgp" % "2.3.2")

//addSbtPlugin("cchantep" % "sbt-hl-compiler" % "0.12")

//addSbtPlugin("cchantep" % "sbt-scaladoc-compiler" % "0.9.1")
