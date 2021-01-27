---
layout: default
---

# Scala-TS

*Scala-TS* is a simple tool which can generate [TypeScript](https://www.typescriptlang.org) types from [Scala](https://www.scala-lang.org/) types.

## Usage

*Scala-TS* can be used as a [SBT plugin](#sbt-plugin) (recommended) or as a Scala [compiler plugin](#compiler-plugin), to handle generate TypeScript on Scala compilation.

*Input Scala:*

```scala
package scalats.docs

case class Incident(id: String, message: String)
```

*Generated TypeScript:*

```typescript
export interface Incident {
  id: string;
  message: string;
}

const incident: Incident = {
  id: 'id',
  message: 'A message'
}
```

> See [more examples](./examples.html)

[![Maven](https://img.shields.io/maven-central/v/io.github.scala-ts/scala-ts-core_{{site.scala_major_version}}.svg)](http://search.maven.org/#search%7Cga%7C1%7Ca%3A%22scala-ts-core_{{site.scala_major_version}}%22)

**Release notes:**

*New version 0.3.2* - added support for more types; added file output support.

*New version 0.4.0* - added support for SBT 1.0, `Either` and `Map`.

### SBT plugin

*Scala-TS* can be used as a [SBT plugin](#sbt-plugin).
If can be set up by adding to `project/plugins.sbt`:

    addSbtPlugin("io.github.scala-ts" % "sbt-scala-ts" % "{{site.latest_release}}")

Additionally, the plugin must be enabled per project.

```ocaml
// Not enabled by default
enablePlugins(TypeScriptGeneratorPlugin)
```

The TypeScript files are generated on compile.

    sbt compile

By default, the TypeScript generation is executed in directory `target/scala-ts/src_managed` (see `sourceManaged in scalatsOnCompile` in [configuration](./config.html)).

> See [SBT plugin settings](./config.html#sbt-plugin-settings)

> See [SBT examples on GitHub](https://github.com/scala-ts/scala-ts/tree/master/sbt-plugin/src/sbt-test/sbt-scala-ts/)

### Compiler plugin

*Scala-TS* can also be configured as a Scalac compiler plugin using the following options.

- `-Xplugin:/path/to/scala-ts-core.jar`
- `-P:scalats:configuration=/path/to/plugin.conf`

The `plugin.conf` is a [HOCON](https://github.com/lightbend/config#using-hocon-the-json-superset) file that define the generator settings (see [example](https://github.com/scala-ts/scala-ts/blob/master/core/src/test/resources/plugin-conf.xml).

> See [compiler settings](./config.html#compiler-settings)

## Type reference

*Scala-TS* can emit TypeScript for different kinds of Scala types declaration (see [examples](#examples)).

| Scala         | TypeScript    |
| ------------- | ------------- |
| Case class    | Interface     |
| Sealed family | Interface     |
| Enumeration   | Enum          |
| Value class   | *Inner value* |

*Scala-TS* support the following scalar types for the members/fields in the transpiled declaration.

| Scala                                             | TypeScript        |
| ------------------------------------------------- | ----------------- |
| `Boolean`                                         | `boolean`         |
| `Byte`, `Double`, `Float`, `Int`, `Long`, `Short` | `number`          |
| `BigDecimal`, `BigInt`, `BigInteger`              | `number`          |
| `String`, `UUID`                                  | `string`          |
| `Date`, `Instant`, `Timestamp`                    | `Date`            |
| `LocalDate`, `LocalDateTime`                      | `Date`            |
| `ZonedDateTime`, `OffsetDateTime`                 | `Date`            |
| `List`, `Seq`, `Set` (any collection)             | `ReadonlyArray`   |
| `Map[K, V]` (`K` as key type, `V` as value type)  | `{ [key: K]: V }` |
| `(A, B)` (`Tuple2` with `A` as first type)        | `[A, B]`          |
| `Tuple3[A, B, C]` (similar for other tuple types) | `[A, B, C]`       |
| `Either[L, R]` (`L` on left, `R` on right)        | `L | R`           |

### Option

By default, according the setting `optionToNullable`, [`Option`](https://www.scala-lang.org/api/current/scala/Option.html) values are generated as omitable TypeScript fields (`T | undefined`) or as nullable fields.

```typescript
// For Option[String]
export interface Example {
  ifOptionToNullable?: string | undefined,
  otherwise: string | nullable
}
```

## Extensions

Additionnally to the *Scala-TS* SBT plugin, some extensions are provided.

### idonttrustlikethat

A SBT plugin extension is provided to generate TypeScript [idonttrustlikethat](https://github.com/AlexGalays/idonttrustlikethat) validators, and derived types.

This can be configured by first configuring `sbt-scala-ts-idtlt` to the `project/plugins.sbt` (instead of base `sbt-scala-ts`).

```ocaml
addSbtPlugin("io.github.scala-ts" % "sbt-scala-ts-idtlt" % {{site.latest_release}})
```

Then in the `build.sbt` is can be configured as below.

```ocaml
enablePlugins(TypeScriptIdtltPlugin) // Required as disabled by default
```

**Example:** Scala case class

```scala
package scalats.docs.idtlt

import java.time.LocalDate

case class Bar(
  name: String,
  age: Int,
  amount: Option[BigInt],
  updated: LocalDate,
  created: LocalDate
)
```

It generates the following TypeScript validators and types.

```typescript
import * as idtlt from 'idonttrustlikethat';

// Validator for InterfaceDeclaration Bar
export const idtltBar = idtlt.object({
  created: idtlt.isoDate,
  updated: idtlt.isoDate,
  amount: idtlt.number.optional(),
  age: idtlt.number,
  name: idtlt.string,
});

export const idtltDiscriminatedBar = idtlt.intersection(
  idtltBar,
  idtlt.object({
    '_type': idtlt.literal('Bar')
  })
);

// Deriving TypeScript type from Bar validator
export type Bar = typeof idtltBar.T;
```
