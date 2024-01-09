ThisBuild / scalafmtOnCompile := true

// Scalafix
inThisBuild({
  if (sys.props.get("scalafix.disable").isEmpty) {
    List(
      // scalaVersion := "2.13.3",
      semanticdbEnabled := true,
      semanticdbVersion := scalafixSemanticdb.revision,
    )
  } else List.empty
})
