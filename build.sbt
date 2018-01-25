scalacOptions in ThisBuild ++= Seq("-unchecked", "-deprecation")

publishArtifact in Test := false

lazy val root = (project in file(".")).
  settings(
    version in ThisBuild := "0.4.1",
    organization in ThisBuild := "com.returntocorp",
    description := "Generate TS models from scala",
    name := "scala-ts",
    sbtPlugin := true,
    licenses += ("MIT", url("http://opensource.org/licenses/MIT")),
    publishMavenStyle := false,
    bintrayRepository := "sbt-plugins",
    mainClass in (Compile, run) := Some("com.mpc.scalats.Main"),
    sbtVersion := "1.0.2",
    bintrayOrganization := Some("returntocorp")
  )
libraryDependencies ++= Seq(
  "org.scala-lang" % "scala-reflect" % "2.10.6",
  "ch.qos.logback" % "logback-classic" % "1.1.7",
  "org.scalatest" %% "scalatest" % "3.0.1" % "test"
)
