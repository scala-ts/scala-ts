# SBT plugin with support for Python generation

## Usage

In `project/plugins.sbt`:

    addSbtPlugin("io.github.scala-ts" % "sbt-scala-ts-python" % pluginVer)

In `build.sbt`:

    enablePlugins(TypeScriptIdtltPlugin) // Not enabled by default
