---
layout: default
---

# Scala-TS

*Scala-TS* is a simple tool which can generate [TypeScript](https://www.typescriptlang.org) types from [Scala](https://www.scala-lang.org/) types.

## Usage

It handles various Scala types.

*Input Scala:*

```scala
package scalats.examples

case class Incident(id: String, message: String)
```

*Generated TypeScript:*

```typescript
export interface Incident {
  id: string;
  message: string;
}
```

> See [more examples](./examples.html)

### SBT plugin

*Scala-TS* can be used as a [SBT plugin](#sbt-plugin).
If can be set up by adding the plugin to `project/plugins.sbt`:

    addSbtPlugin("io.github.scala-ts" % "scala-ts-sbt" % "{{site.latest_release}}")

Additionally, enable the plugin for a specific project:

```ocaml
// Disabled by default
enablePlugins(io.github.scalats.sbt.TypeScriptGeneratorPlugin)
```

The TypeScript files are generated on compile.

    sbt compile

By default, the TypeScript generation is executed in directory `target/scala-ts/src_managed` (see `sourceManaged in scalatsOnCompile` in [configuration](#configuration)).

> See [SBT examples on GitHub](https://github.com/scala-ts/scala-ts/tree/master/sbt-plugin/src/sbt-test/scala-ts-sbt/)

**Release notes:**

*New version 0.3.2* - added support for more types; added file output support.

*New version 0.4.0* - added support for SBT 1.0, Either and Map.

#### Configuration

The [compiler plugin settings](#compiler-plugin) can be configured as SBT settings, using the `scalats` prefix; e.g. The `scalatsTypescriptIndent` SBT setting corresponds to the compiler plugin setting `typescriptIndent`.

```ocaml
scalatsTypescriptIndent := "\t"
```

The SBT plugins also has some specific settings.

TODO: Review

TODO: scalatsOnCompile (default true)

TODO: scalatsDebug false

TODO: `sourceManaged in scalatsOnCompile` - the directory to initialize the printer with (output directory)

TODO: Custom field naming in `project/` + `scalatsTypeScriptFieldMapper := classOf[scalats.CustomTypeScriptFieldMapper]`

TODO: Custom printer in `project/`

### Compiler plugin

*Scala-TS* can be configured as a Scalac compiler plugin using the following options.

- `-Xplugin:/path/to/scala-ts-core.jar`
- `-P:scalats:configuration=/path/to/plugin.conf`

The following generator settings can be specified as [HOCON](https://github.com/lightbend/config#using-hocon-the-json-superset) in the plugin configuration (see [examples](../core/src/test/resources/plugin-conf.xml)).

TODO: TypeScriptTypeMapper
TODO: TypeScriptFieldMapper
TODO: TypeScriptDeclarationMapper = enumerationAsEnum, singletonAsLiteral, scalatsUnionAsSimpleUnion, scalatsUnionWithLiteral

- `optionToNullable` - Translate `Option` types to union type with `null` (e.g. `Option[Int]` to `number | null`); Default `false` as builtin behaviour is option-to-undefined (see `TypeScriptTypeMapper.NullableAsOption`).
- `prependEnclosingClassNames` - Prepend the name of enclosing classes to the generated types (default: `true`)
- `typescriptIndent` - The characters used as TypeScript indentation (default: 2 spaces).
- `typescriptLineSeparator` - The characters used to separate TypeScript line/statements (default: `;`).
- `typeNaming` - The conversions for the type names (default: `Identity`).
- `fieldMapper` - The conversions for the field names if emitCodecs: `Identity`, `SnakeCase` or a class name (default: `Identity`).
- `discriminator` - The name of the field to be used as discriminator (default: `_type`).

Also the following build options can be configured.

TODO:
- `printer` - An optional printer class.
- `additionalClasspath` - A list of URL to be added to the plugin classpath (to be able to load `fieldNaming` or `printer` from).

- `compilationRuleSet` - Set of rules to specify which Scala source files must be considered.
- `typeRuleSet` - Set of rules to specify which types (from the already filtered source files) must be considered.

A rule set such as `compilationRuleSet` is described with multiple include and/or excludes rules:

```
compilationRuleSet {
   includes = [ "ScalaParserSpec\\.scala", "Transpiler.*" ]
   excludes = [ "foo" ]
}

typeRuleSet {
  # Regular expressions on type full names.
  # Can be prefixed with either 'object:' or 'class:' (for class or trait).
  includes = [ "org\\.scalats\\.core\\..*" ]

  excludes = [
    ".*Spec", 
    "ScalaRuntimeFixtures$", 
    "object:.*ScalaParserResults", 
    "FamilyMember(2|3)"
  ]
}
```

Optionally the following argument can be passed.

- `-P:scalats:debug` - Enable debug.
- `-P:scalats:printerOutputDirectory=/path/to/base` - Path to a base directory to initialize a custom printer with.
- `-P:scalats:sys.scala-ts.printer.prelude-url=/path/to/prelude` - Set the system property (`scala-ts.printer.prelude-url`) to pass `/path/to/prelude` as printer prelude.

## Type reference

*Scala-TS* can emit TypeScript for different kinds of Scala types declaration (see [examples](#examples)).

| Scala         | TypeScript    |
| ------------- | ------------- |
| Case class    | Interface     |
| Sealed family | Interface     |
| Enumeration   | Enum          |
| Value class   | *Inner value* |

*Scala-TS* support the following scalar types for the members/fields in the transpiled declaration.

| Scala                                             | TypeScript |
| ------------------------------------------------- | ---------- |
| `Boolean`                                         | `boolean`  |
| `Byte`, `Double`, `Float`, `Int`, `Long`, `Short` | `number`   |
| `BigDecimal`, `BigInt`, `BigInteger`              | `number`   |
| `String`, `UUID`                                  | `string`   |
| `Date`, `Instant`, `Timestamp`                    | `Date`     |
| `LocalDate`, `LocalDateTime`                      | `Date`     |
| `ZonedDateTime`, `OffsetDateTime`                 | `Date`     |

TODO:
- `List`, `Seq`, `Set`, `Map`, `Tuple`
- `Option`, `Either`