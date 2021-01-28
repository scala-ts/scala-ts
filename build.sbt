organization := "io.github.scala-ts"

name := "scala-ts-demo"

// Run options
run / fork := true

Global / cancelable := false

run / javaOptions += "-DstopOnEOF=true"

run / connectInput := true

// Scala-TS configuration
enablePlugins(TypeScriptGeneratorPlugin)

scalatsUnionWithLiteral

scalatsSourceIncludes := Set(".*/model/.*\\.scala")

// Format and style
scalafmtOnCompile := true

inThisBuild(
  List(
    //scalaVersion := "2.13.3",
    semanticdbEnabled := true,
    semanticdbVersion := scalafixSemanticdb.revision,
    scalafixDependencies ++= Seq(
      "com.github.liancheng" %% "organize-imports" % "0.4.2"),
  )
)

// Packaging
enablePlugins(JavaAppPackaging)

// Logging
libraryDependencies += "ch.qos.logback" % "logback-classic" % "1.2.3"

// Akka
val akkaVer = "2.6.9"
val akkaHttpVersion = "10.2.0"

libraryDependencies ++= Seq(
  "com.typesafe.akka" %% "akka-stream" % akkaVer,
  "com.typesafe.akka" %% "akka-slf4j" % akkaVer,
  "com.typesafe.akka" %% "akka-http" % akkaHttpVersion,
  "ch.megard" %% "akka-http-cors" % "1.1.0",
  "de.heikoseeberger" %% "akka-http-play-json" % "1.35.3",
  "com.typesafe.akka" %% "akka-stream-testkit" % akkaVer % Test)
