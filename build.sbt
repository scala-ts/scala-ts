import sbt.Keys._





licenses += ("MIT", url("http://opensource.org/licenses/MIT"))

scalacOptions in ThisBuild ++= Seq("-unchecked", "-deprecation")

publishMavenStyle := true

publishArtifact in Test := false

pomIncludeRepository := { _ => false }

lazy val pomSettings = Seq(
  publishMavenStyle := true,
  bintrayOrganization := Some("returntocorp"),
  bintrayRepository := "maven",
  publishArtifact in Test := false,
  pomExtra :=
    <url>https://github.com/returntocorp/scala-ts</url>
    <licenses>
      <license>
        <name>MIT</name>
        <url>https://opensource.org/licenses/MIT</url>
        <distribution>repo</distribution>
      </license>
    </licenses>
    <scm>
      <url>git@github.com:returntocorp/scala-ts.git</url>
      <connection>scm:git:git@github.com:returntocorp/scala-ts.git</connection>
    </scm>
    <developers>
      <developer>
        <id>miloszpp</id>
        <name>Miłosz Piechocki</name>
        <url>http://codewithstyle.info</url>
      </developer>
    </developers>
)



lazy val root = (project in file(".")).
  settings(
    name := "scala-ts",
    version := "0.4.0",
    organization := "com.returntocorp",
    scalaVersion := "2.12.4",
    mainClass in (Compile, run) := Some("com.mpc.scalats.Main"),
    sbtPlugin := true,
    sbtVersion := "1.0.2"
  ).
  settings(pomSettings)

libraryDependencies ++= Seq(
  "org.scala-lang" % "scala-reflect" % "2.10.6",
  "ch.qos.logback" % "logback-classic" % "1.1.7",
  "org.scalatest" %% "scalatest" % "3.0.1" % "test"
)
