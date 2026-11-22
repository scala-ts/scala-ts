ThisBuild / scalafmtOnCompile := true

// Scalafix
if (!sys.props.get("scalafix.disable").contains("true")) {
  inThisBuild(
    List(
      // scalaVersion := "2.13.3",
      semanticdbEnabled := true,
      semanticdbVersion := scalafixSemanticdb.revision
    )
  ) ++ Seq(
    Compile / semanticdbTargetRoot := (Compile / classDirectory).value,
    Test / semanticdbTargetRoot := (Test / classDirectory).value
  )
} else Seq.empty
