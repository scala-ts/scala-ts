ThisBuild / scalaVersion := "2.13.7"

ThisBuild / scalacOptions ++= Seq(
  "-encoding",
  "UTF-8",
  "-unchecked",
  "-deprecation",
  "-feature",
  // "-language:higherKinds",
  "-Xfatal-warnings",
  "-Xlint",
  "-Ywarn-numeric-widen",
  "-Ywarn-dead-code",
  "-Ywarn-value-discard",
  "-Wmacros:after",
  "-Wunused",
  "-g:vars"
)

Test / scalacOptions ~= {
  _.filterNot(_ == "-Xfatal-warnings")
}

Compile / console / scalacOptions ~= {
  _.filterNot { opt => opt.startsWith("-X") || opt.startsWith("-Y") }
}

Test / console / scalacOptions ~= {
  _.filterNot { opt => opt.startsWith("-X") || opt.startsWith("-Y") }
}

// Silencer
ThisBuild / libraryDependencies ++= {
  val silencerVersion = "1.7.8"

  Seq(
    compilerPlugin(
      ("com.github.ghik" %% "silencer-plugin" % silencerVersion)
        .cross(CrossVersion.full)
    ),
    ("com.github.ghik" %% "silencer-lib" % silencerVersion % Provided)
      .cross(CrossVersion.full)
  )
}
