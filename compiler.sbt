ThisBuild / scalaVersion := "2.13.18"

val scala3Lts = "3.3.7"

ThisBuild / crossScalaVersions := Seq((ThisBuild / scalaVersion).value, scala3Lts)

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
  val v = scalaBinaryVersion.value

  if (!v.startsWith("3")) {
    val silencerVersion: String = {
      if (v == "2.11") {
        "1.17.13"
      } else {
        "1.7.19"
      }
    }

    Seq(
      compilerPlugin(
        ("com.github.ghik" %% "silencer-plugin" % silencerVersion)
          .cross(CrossVersion.full)
      ),
      ("com.github.ghik" %% "silencer-lib" % silencerVersion % Provided)
        .cross(CrossVersion.full)
    )
  } else Seq.empty
}
