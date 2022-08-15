ThisBuild / scalaVersion := "2.13.8"

ThisBuild / crossScalaVersions := Seq((ThisBuild / scalaVersion).value, "3.1.3")

ThisBuild / scalacOptions ++= Seq(
  "-encoding",
  "UTF-8",
  "-unchecked",
  "-deprecation",
  "-feature"
  // "-language:higherKinds",
)

scalacOptions ++= {
  if (scalaBinaryVersion.value == "3") {
    Seq.empty
  } else {
    Seq(
      "-Xfatal-warnings",
      "-Xlint",
      "-Ywarn-numeric-widen",
      "-Ywarn-dead-code",
      "-Ywarn-value-discard",
      "-Wmacros:after",
      "-Wunused",
      "-g:vars"
    )
  }
}

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
  if (scalaBinaryVersion.value == "3") {
    Seq.empty
  } else {
    val silencerVersion = "1.7.9"

    Seq(
      compilerPlugin(
        ("com.github.ghik" %% "silencer-plugin" % silencerVersion)
          .cross(CrossVersion.full)
      ),
      ("com.github.ghik" %% "silencer-lib" % silencerVersion % Provided)
        .cross(CrossVersion.full)
    )
  }
}
