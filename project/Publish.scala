import sbt._
import sbt.Keys._
import sbt.plugins.JvmPlugin

object Publish extends AutoPlugin {
  override def trigger = allRequirements
  override def requires = JvmPlugin

  private val repoName = env("PUBLISH_REPO_NAME")
  private val repoUrl = env("PUBLISH_REPO_URL")

  override lazy val projectSettings = Seq(
    licenses := Seq(
      "MIT" -> url("https://opensource.org/licenses/MIT")),
    publishMavenStyle := true,
    Test / publishArtifact := false,
    publishTo := Some(repoUrl).map(repoName at _),
    credentials += Credentials(repoName, env("PUBLISH_REPO_ID"),
      env("PUBLISH_USER"), env("PUBLISH_PASS")),
    homepage := Some(url("https://scala-ts.github.io/scala-ts/")),
    autoAPIMappings := true,
    pomExtra :=
      <scm>
        <url>git@github.com:scala-ts/scala-ts.git</url>
        <connection>scm:git:git@github.com:scala-ts/scala-ts.git</connection>
      </scm>
      <developers>
        <developer>
          <id>miloszpp</id>
          <name>Miłosz Piechocki</name>
          <url>http://codewithstyle.info</url>
        </developer>
        <developer>
          <id>cchantep</id>
          <name>cchantep</name>
          <url>https://github.com/cchantep</url>
        </developer>
      </developers>
  )

  @inline private def env(n: String): String = sys.env.get(n).getOrElse(n)
}
