# SBT plugin with support for idonttrustlikethat

## Usage

In `project/plugins.sbt`:

    addSbtPlugin("io.github.scala-ts" % "sbt-scala-ts-idtlt" % pluginVer)

In `build.sbt`:

    enablePlugins(TypeScriptIdtltPlugin) // Not enabled by default
