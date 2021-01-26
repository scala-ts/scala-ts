# SBT plugin with support for idonttrustlikethat

## Usage

In `project/plugins.sbt`:

    addSbtPlugin("io.github.scala-ts" %% "scala-ts-sbt-idtlt" % pluginVer)

In `build.sbt`:

    enablePlugins(TypeScriptIdtltPlugin) // Not enabled by default