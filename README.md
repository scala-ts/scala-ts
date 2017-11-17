# scala-ts

# quickstart for development

    sbt
    runMain com.mpc.scalats.AuthorExample

# readme

*scala-ts* is a simple tool which can generate TypeScript interfaces and classes from Scala case classes.

*scala-ts* is helpful when working with REST-ful Scala backend and TypeScript frontend. Having defined Scala types returned by your endpoints you can easily generate TypeScript definitions for consuming these endpoints.

http://codewithstyle.info/scala-ts-scala-typescript-code-generator/

*New version 0.3.2* - added support for more types; added file output support.

## Usage

*scala-ts* can be used either standalone or as a sbt plugin.

### Standalone

Run `com.mpc.scalats.Main` class and provide a space separated list of fully qualified class names which you want to generate TypeScript for. 

Example:
```
java -cp 'scala-ts-assembly-0.1.0.jar' com.mpc.scalats.Main "com.example.ExampleDto"
```

### SBT plugin

Add the following plugin to `plugins.sbt`:
```
addSbtPlugin("com.github.miloszpp" % "scala-ts" % "0.3.0")
```

Additionally, enable the plugin in your project settings:
```
enablePlugins(com.mpc.scalats.sbt.TypeScriptGeneratorPlugin)
```

Now you can use the `generateTypeScript` command in SBT. For example:
```
sbt "generateTypeScript com.example.ExampleDto"
```
### Configuration

Starting from release 0.3.0, it's possible to specify some configuration options:
* `emitInterfaces` - generate interface declarations (`true` by default)
* `emitClasses` - generate class declarations (`false` by default)
* `optionToNullable` - translate `Option` types to union type with `null` (e.g. `Option[Int]` to `number | null`)
* `optionToUndefined` - translate `Option` types to union type with `undefined` (e.g. `Option[Int]` to `number | undefined`) - can be combined with `optionToNullable`
* `outputStream` - the stream to which the code should be emitted; it defaults to console

Usage example in `build.sbt`:
```
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

## Credits

Many thanks to https://github.com/nicolasdalsass who forked the project into https://github.com/Elium/scala-ts/tree/master. I incorporated some of his ideas into `scala-ts`.
