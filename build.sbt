organization := "io.github.scala-ts"

// Format and style
scalafmtOnCompile := true

inThisBuild(
  List(
    resolvers += Resolver.sonatypeRepo("staging"),
    //scalaVersion := "2.13.3",
    semanticdbEnabled := true,
    semanticdbVersion := scalafixSemanticdb.revision,
    scalafixDependencies ++= Seq(
      "com.github.liancheng" %% "organize-imports" % "0.4.2"
    )
  )
)

// Common model
lazy val common = (project in file("common"))
  .enablePlugins(TypeScriptIdtltPlugin)
  .settings(
    name := "scala-ts-demo-common",
    sourceManaged in scalatsOnCompile := {
      val dir = baseDirectory.value / ".." / "frontend" / "src" / "_generated"
      dir.mkdirs()
      dir
    }
  )

// Scala Akka-HTTP API
lazy val `http-api` = (project in file("http-api"))
  .enablePlugins(JavaAppPackaging)
  .settings(
    name := "scala-ts-demo-api",
    // Run options
    run / fork := true,
    Global / cancelable := false,
    run / javaOptions += "-DstopOnEOF=true",
    run / connectInput := true,
    libraryDependencies ++= {
      // Versions
      val akkaVer = "2.6.9"
      val akkaHttpVersion = "10.2.3"

      Seq(
        // Logging
        "ch.qos.logback" % "logback-classic" % "1.2.3",
        // As in memory DB
        "com.google.guava" % "guava" % "30.1-jre",
        // Akka
        "com.typesafe.akka" %% "akka-stream" % akkaVer,
        "com.typesafe.akka" %% "akka-slf4j" % akkaVer,
        "com.typesafe.akka" %% "akka-http" % akkaHttpVersion,
        "ch.megard" %% "akka-http-cors" % "1.1.3",
        "de.heikoseeberger" %% "akka-http-play-json" % "1.39.2",
        "com.typesafe.akka" %% "akka-stream-testkit" % akkaVer % Test
      )
    }
  )
  .dependsOn(common)

lazy val root = (project in file("."))
  .settings(
    name := "scala-ts-demo",
    publish := ({}),
    publishTo := None
  )
  .aggregate(`http-api`)
