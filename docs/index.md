---
layout: default
---

# Scala-TS

*Scala-TS* is a simple tool which can generate [TypeScript](https://www.typescriptlang.org) types from [Scala](https://www.scala-lang.org/) types.

## Usage

*Scala-TS* can be used as a [SBT plugin](#sbt-plugin).

### Examples

*Scala-TS* handle various cases and can be configured in many ways.

#### Example 1

A simple [case class](https://docs.scala-lang.org/tour/case-classes.html) `Incident`.

```scala
package scalats.examples

case class Incident(id: String, message: String)
```

*Generated TypeScript:* [Interface](https://www.typescriptlang.org/docs/handbook/interfaces.html) `Incident`

```typescript
export interface Incident {
  id: string;
  message: string;
}
```

#### Example 2

Case class `Station` with `Option`al field `lastIncident`.

```scala
package scalats.examples

case class Station(
    id: String,
    name: String,
    lastIncident: Option[Incident])
```

*Generated TypeScript:*

```typescript
export interface Station {
  id: string;
  name: string;
  lastIncident?: Incident;
}
```

> See `TypeScriptTypeMapper.NullableAsOption` with setting `typeScriptTypeMappers` bellow in [configuration](#Configuration) documentation.

#### Example 3

Generic case class `Tagged[T]`.

```scala
package scalats.examples

case class Tagged[T](tag: String, value: T)
```

*Generated TypeScript:*

```typescript
export interface Tagged<T> {
  tag: string;
  value: T;
}
```

#### Example 4

Related case classes `Event` and `Message`, also using the previous generic type `Tagged` and [Value class](https://docs.scala-lang.org/overviews/core/value-classes.html) `EventType`.

```scala
package scalats.examples

import java.util.Locale
import java.time.OffsetDateTime

final class EventType(val name: String) extends AnyVal

case class Event(
    id: String,
    changed: OffsetDateTime,
    `type`: EventType,
    messages: Tagged[Seq[TextMessage]])

case class TextMessage(
    format: String,
    language: Locale,
    text: String)
```

*Generated TypeScript:* Note that the Value class `EventType` is exported as the inner type (there `string` for `val name: String`).

```typescript
export interface Event {
  id: string;
  changed: Date;
  type: string;
  messages: Tagged<ReadonlyArray<Message>>;
}

export interface Message {
  format: string;
  language: string;
  text: string;
}
```

> `Locale` type is provided a transpiler as `string`.

#### Example 5

[Sealed trait](https://docs.scala-lang.org/tour/traits.html#subtyping)/family `Transport`; By default, sealed trait is generated using inheritance.

```scala
package scalats.examples

sealed trait Transport {
  def name: String
}

case class TrainLine(
    name: String,
    startStationId: String,
    endStationId: String)
    extends Transport

case class BusLine(
    id: Int,
    name: String,
    stopIds: Seq[String])
    extends Transport
```

*Generated TypeScript:*

```typescript
export interface TrainLine extends Transport {
  name: string;
  startStationId: string;
  endStationId: string;
}

export interface BusLine extends Transport {
  id: number;
  name: string;
  stopIds: ReadonlyArray<string>;
}

export interface Transport {
  name: string;
}
```

> See on [GitHub](https://github.com/scala-ts/scala-ts/tree/master/sbt-plugin/src/sbt-test/scala-ts-sbt/simple/)

#### Example 6

Sealed trait/family as [TypeScript union type](https://www.typescriptlang.org/docs/handbook/unions-and-intersections.html).

Using `scalatsUnionWithLiteral` settings (which setup appropriate declaration mapper and import resolvers), a Scala sealed family representing a union type can be generated as TypeScript union type.

```scala
package scalats.examples

sealed trait Greeting

object Greeting {
  case object Hello extends Greeting
  case object GoodBye extends Greeting
  case object Hi extends Greeting
  case object Bye extends Greeting

  case class Whatever(word: String) extends Greeting
}
```

*Generated TypeScript:*

```typescript
export const ByeInhabitant = 'Bye';

export type Bye = typeof ByeInhabitant;

export const GoodByeInhabitant = 'GoodBye';

export type GoodBye = typeof GoodByeInhabitant;

export const HelloInhabitant = 'Hello';

export type Hello = typeof HelloInhabitant;

export const HiInhabitant = 'Hi';

export type Hi = typeof HiInhabitant;

export interface Whatever {
  word: string;
}

export type Greeting = Bye | GoodBye | Hello | Hi | Whatever;
```

> See `scalatsUnionWithLiteral` SBT settings, `TypeScriptDeclarationMapper.SingletonAsLiteral` and `TypeScriptDeclarationMapper.UnionAsSimpleUnion` for setting `typeScriptDeclarationMappers`, and `TypeScriptImportResolver.UnionWithLiteralSingleton` and `typeScriptImportResolvers` setting; Details bellow in [configuration](#Configuration) documentation.

> See on [GitHub](https://github.com/scala-ts/scala-ts/tree/master/sbt-plugin/src/sbt-test/scala-ts-sbt/enumeratum/) (example with [Enumeratum](https://github.com/lloydmeta/enumeratum#enumeratum------))

#### Example 7

[Scala Enumeration](https://www.scala-lang.org/api/current/scala/Enumeration.html).

```scala
package scalats.examples

object WeekDay extends Enumeration {
  type WeekDay = Value
  val Mon, Tue, Wed, Thu, Fri, Sat, Sun = Value
}
```

*Generated TypeScript:* [union](https://www.typescriptlang.org/docs/handbook/unions-and-intersections.html) `WeekDay`

```typescript
export type WeekDay = 'Mon' | 'Tue' | 'Wed' | 'Thu' | 'Fri' | 'Sat' | 'Sun'

export const WeekDayValues = [ 'Mon', 'Tue', 'Wed', 'Thu', 'Fri', 'Sat', 'Sun' ]
// Useful to iterate the values
```

> See on [GitHub](https://github.com/scala-ts/scala-ts/tree/master/sbt-plugin/src/sbt-test/scala-ts-sbt/single-file-printer/)

#### Example 8

Scala [Singleton objects](https://docs.scala-lang.org/tour/singleton-objects.html) as TypeScript [Literal types](https://www.typescriptlang.org/docs/handbook/literal-types.html).

```scala
package scalats.examples

sealed abstract class State(val entryName: String)

case object Alabama extends State("AL")
case object Alaska extends State("AK")
```

*Generated TypeScript:* `entryName` is transpiled as literal.

```
export const AlabamaInhabitant = "AL";

export type Alabama = typeof AlabamaInhabitant;

export const AlaskaInhabitant = "AK";

export type Alaska = typeof AlaskaInhabitant;
```

> See on [GitHub](https://github.com/scala-ts/scala-ts/tree/master/sbt-plugin/src/sbt-test/scala-ts-sbt/enumeratum/)

### SBT plugin

Add the following plugin to `project/plugins.sbt`:

    addSbtPlugin("io.github.scala-ts" % "scala-ts-sbt" % "{{site.latest_release}}")

Additionally, enable the plugin for a specific project:

```ocaml
// Disabled by default
enablePlugins(io.github.scalats.sbt.TypeScriptGeneratorPlugin)
```

The TypeScript files are generated on compile.

    sbt compile

By default, the TypeScript generation is executed in directory `target/scala-ts/src_managed` (see `sourceManaged in scalatsOnCompile` in [configuration](#configuration)).

*See [examples](https://github.com/scala-ts/scala-ts/tree/master/sbt-plugin/src/sbt-test/scala-ts-sbt)*

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

- `optionToNullable` - Translate `Option` types to union type with `null` (e.g. `Option[Int]` to `number | null`)
- `optionToUndefined` - Translate `Option` types to union type with `undefined` (e.g. `Option[Int]` to `number | undefined`) - can be combined with `optionToNullable`
- `prependIPrefix` - Prepend `I` prefix to generated interfaces (default: `true`)
- `prependEnclosingClassNames` - Prepend the name of enclosing classes to the generated types (default: `true`)
- `typescriptIndent` - The characters used as TypeScript indentation (default: 2 spaces).
- `typescriptLineSeparator` - The characters used to separate TypeScript line/statements (default: `;`).
- `fieldNaming` - The conversions for the field names if emitCodecs: `Identity`, `SnakeCase` or a class name (default: `Identity`).
- `printer` - An optional printer class.
- `additionalClasspath` - A list of URL to be added to the plugin classpath (to be able to load `fieldNaming` or `printer` from).

Also the following build options can be configured.

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


- `List`, `Seq`, `Set`, `Map`, `Tuple`
- `Option`, `Either`

TODO: Table for mapping between Scala / TS types
TODO: TypeScriptTypeMapper
TODO: TypeScriptDeclarationMapper = enumerationAsEnum, singletonAsLiteral, scalatsUnionAsSimpleUnion, scalatsUnionWithLiteral