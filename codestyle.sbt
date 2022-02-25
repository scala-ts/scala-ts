ThisBuild / scalafmtOnCompile := true

// Scalafix
inThisBuild({
  if (sys.props.get("scalafix.disable").isEmpty) {
    List(
      // scalaVersion := "2.13.3",
      semanticdbEnabled := true,
      semanticdbVersion := scalafixSemanticdb.revision,
      scalafixDependencies ++= Seq(
        "com.github.liancheng" %% "organize-imports" % "0.6.0"
      )
    )
  } else List.empty
})
