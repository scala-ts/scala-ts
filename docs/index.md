---
layout: default
---

# Scala-ts

*scala-ts* is a simple tool which can generate TypeScript interfaces and classes from Scala case classes.

## Release notes

*New version 0.3.2* - added support for more types; added file output support.

*New version 0.4.0* - added support for SBT 1.0, Either and Map.

## Usage

*scala-ts* can be used either standalone or as a sbt plugin.

### Standalone

A standalone assembly can be directly downloaded from the corresponding [release](https://github.com/scala-ts/scala-ts/releases), and executed from CLI:

    java -jar "/path/to/scala-ts-assembly-$VERSION.jar" "com.example.ExampleDto"

In previous example, `com.example.ExampleDto` is the Scala class for which the TypeScript must be generated.

### SBT plugin

Add the following plugin to `project/plugins.sbt`:

    addSbtPlugin("com.github.miloszpp" % "scala-ts" % version)

Additionally, enable the plugin in your project settings:

    enablePlugins(com.mpc.scalats.sbt.TypeScriptGeneratorPlugin)

Now you can use the `generateTypeScript` command in SBT. For example:

    sbt "generateTypeScript com.example.ExampleDto"

### Configuration

Starting from release 0.3.0, it's possible to specify some configuration options:

* `emitInterfaces` - generate interface declarations (`true` by default)
* `emitClasses` - generate class declarations (`false` by default)
* `emitCodecs` - generate fromData/toData functions for TypeScript classes (if `emitClasses`)
* `optionToNullable` - translate `Option` types to union type with `null` (e.g. `Option[Int]` to `number | null`)
* `optionToUndefined` - translate `Option` types to union type with `undefined` (e.g. `Option[Int]` to `number | undefined`) - can be combined with `optionToNullable`
* `outputStream` - the stream to which the code should be emitted; it defaults to console
* `typescriptIndent` - the characters used as TypeScript indentation (default: <tab>)
* `fieldNaming` - the conversions for the field names if emitCodecs (default: FieldNaming.Identity)

Usage example in `build.sbt`:

```ocaml
emitClasses in generateTypeScript := true

enablePlugins(com.mpc.scalats.sbt.TypeScriptGeneratorPlugin)
```

## Type support

Currently *scala-ts* supports the following types of case class members:

* `Int`, `Double`, `Boolean`, `String`, `Long`
* `List`, `Seq`, `Set`
* `Option`
* `LocalDate`, `Instant`, `Timestamp`
* generic types
* References to other case classes
* (case) object, as singleton class
* sealed trait, as union type
