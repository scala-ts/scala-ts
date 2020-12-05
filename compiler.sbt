ThisBuild / scalaVersion := "2.12.12"

ThisBuild / crossScalaVersions := Seq(
  "2.11.12", scalaVersion.value, "2.13.3")

crossVersion := CrossVersion.binary

ThisBuild / scalacOptions ++= Seq(
  "-encoding", "UTF-8", "-target:jvm-1.8",
  "-unchecked",
  "-deprecation",
  "-feature",
  "-Xlint",
  "-g:vars"
  //TODO:"-Xfatal-warnings"
)

ThisBuild / scalacOptions ++= {
  if (scalaBinaryVersion.value == "2.12") {
    Seq(
      "-Xmax-classfile-name", "128",
      "-Ywarn-numeric-widen",
      "-Ywarn-dead-code",
      "-Ywarn-value-discard",
      "-Ywarn-infer-any",
      "-Ywarn-unused",
      "-Ywarn-unused-import",
      "-Ywarn-macros:after"
    )
  } else if (scalaBinaryVersion.value == "2.11") {
    Seq(
      "-Xmax-classfile-name", "128",
      "-Yopt:_", "-Ydead-code", "-Yclosure-elim", "-Yconst-opt")
  } else {
    Seq(
      "-explaintypes",
      //TODO:"-Werror",
      "-Wnumeric-widen",
      "-Wdead-code",
      "-Wvalue-discard",
      "-Wextra-implicit",
      "-Wmacros:after",
      "-Wunused")
  }
}

scalacOptions in (Compile, console) ~= {
  _.filterNot(o => o.startsWith("-X") || o.startsWith("-Y"))
}

scalacOptions in Test ~= {
  _.filterNot(_ == "-Xfatal-warnings")
}

scalacOptions in (Compile, console) ~= {
  _.filterNot { opt => opt.startsWith("-X") || opt.startsWith("-Y") }
}

scalacOptions in (Test, console) ~= {
  _.filterNot { opt => opt.startsWith("-X") || opt.startsWith("-Y") }
}
