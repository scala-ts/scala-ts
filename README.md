# scala-ts

*scala-ts* is a simple tool which can generate TypeScript interfaces and classes from Scala case classes.

*scala-ts* is helpful when working with REST-ful Scala backend and TypeScript frontend. Having defined Scala types returned by your endpoints you can easily generate TypeScript definitions for consuming these endpoints.

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
addSbtPlugin("com.mpc" % "scala-ts" % "0.1.0")
```

Now you can use the `generateTypeScript` command in SBT. For example:
```
sbt "generateTypeScript com.example.ExampleDto"
```

## Type support

Currently *scala-ts* supports the following types of case class members:
* `Int`
* `String`
* `List` and `Seq`
* `LocalDate`
* `Instant`
* References to other case classes
