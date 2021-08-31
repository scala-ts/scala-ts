---
layout: default
---

# Scala-TS

*Scala-TS* is a simple tool which generates [TypeScript](https://www.typescriptlang.org) types from [Scala](https://www.scala-lang.org/) types, to easily integrate REST-ful Scala backends and TypeScript frontends.

> See [release notes](./release-notes.html)

## Usage

*Scala-TS* can be used as a [SBT plugin](#sbt-plugin) (recommended) or as a Scala [compiler plugin](#compiler-plugin), to handle generate TypeScript on Scala compilation.

![Akka HTTP & Svelte example](assets/demo-akka-http-svelte/components.svg)

*Scala-TS* is not [Scala.js](https://www.scala-js.org/); Scala.js is a great tool to port Scala code to JavaScript, so it can be executed as any JS functions, whereas Scala-TS is focused on transpiling only data model types (so the frontend itself is coded in TypeScript, not in Scala).

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

// Can be used...
const incident: Incident = {
  id: 'id',
  message: 'A message'
}
```

> See [examples](./examples.html)

**More:**

- [How to share data model between Akka HTTP API and TypeScript Svelte frontend](./articles/demo-akka-http-svelte.html)
  - Similar [demo with idonttrustlikethat](./articles/demo-idtlt.html)

### SBT plugin

*Scala-TS* can be used as a [SBT plugin](#sbt-plugin).
It can be set up by adding to `project/plugins.sbt`:

    addSbtPlugin("io.github.scala-ts" % "sbt-scala-ts" % "{{site.latest_release}}")

Additionally, the plugin can be enabled per project.

```ocaml
// Not enabled by default
enablePlugins(TypeScriptGeneratorPlugin)
```

The TypeScript files are generated at compile-time.

    sbt compile

By default, the TypeScript generation is executed in directory `target/scala-ts/src_managed` (see `sourceManaged in scalatsOnCompile` in [configuration](./config.html)).

> See [SBT plugin settings](./config.html#sbt-plugin-settings)

> See [SBT examples on GitHub](https://github.com/scala-ts/scala-ts/tree/master/sbt-plugin/src/sbt-test/sbt-scala-ts/)

### Compiler plugin

*Scala-TS* can also be configured as a Scalac compiler plugin using the following options.

- `-Xplugin:/path/to/scala-ts-core.jar`
- `-P:scalats:configuration=/path/to/plugin.conf`

The `plugin.conf` is a [HOCON](https://github.com/lightbend/config#using-hocon-the-json-superset) file that define the generator settings (see [example](https://github.com/scala-ts/scala-ts/blob/master/core/src/test/resources/plugin.conf).

> See [compiler settings](./config.html#compiler-settings)

## Type reference

*Scala-TS* can emit TypeScript for different kinds of Scala types declaration (see [examples](./examples.html)).

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

*See [demo with idonttrustlikethat](./articles/demo-idtlt.html)*
