---
layout: default
---

# Scala-ts

*scala-ts* is a simple tool which can generate [TypeScript](https://www.typescriptlang.org) types from [Scala](https://www.scala-lang.org/) types.

## Usage

*scala-ts* can be used either [standalone](#standalone) or as a [SBT plugin](#sbt-plugin).

### Examples

**Example #1:** Simple [case class](https://docs.scala-lang.org/tour/case-classes.html) `Incident`.

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

**Example #2:** Case class `Station` with `Option`al field `lastIncident`.

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

> See settings `optionToNullable` bellow in [configuration](#Configuration) documentation.

**Example #:** Generic case class `Tagged[T]`.

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

**Example #:** Related case classes `Event` and `Message`, also using the previous generic type `Tagged` and Value class `EventType`.

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

*Generated TypeScript:* Note that the [Value class] `EventType` is exported as the inner type (there `string` for `val name: String`).

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

**Example #:** Sealed trait/family `Transport`

```scala
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

TODO: Scala to TS examples: enumeration

### SBT plugin

Add the following plugin to `project/plugins.sbt`:

    addSbtPlugin("io.github.scala-ts" % "scala-ts-sbt" % "{{site.latest_release}}")

Additionally, enable (or disabled) the plugin for a specific project:

```ocaml
// Enable:
enablePlugins(io.github.scalats.sbt.TypeScriptGeneratorPlugin)
```

By default, the TypeScript files are generated on compile:

    sbt compile

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

*scala-ts* can be configured as a Scalac compiler plugin using the following options.

- `-Xplugin:/path/to/scala-ts-core.jar`
- `-P:scalats:configuration=/path/to/plugin-conf.xml`

The following generator settings can be specified as XML in the plugin configuration (see [examples](../core/src/test/resources/plugin-conf.xml)).

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

```xml
<scalats>
  <compilationRuleSet>
    <includes>
      <include>ScalaParserSpec\.scala</include>
      <include>Transpiler.*</include>
    </includes>

    <excludes>
      <exclude>foo</exclude>
    </excludes>
  </compilationRuleSet>
</scalats>
```

Optionally the following argument can be passed.

- `-P:scalats:debug` - Enable debug.
- `-P:scalats:printerOutputDirectory=/path/to/base` - Path to a base directory to initialize a custom printer with.

### Standalone

A standalone assembly can be directly downloaded from the corresponding [release](https://github.com/scala-ts/scala-ts/releases), and executed from CLI:

    java -jar "/path/to/scala-ts-assembly-$VERSION.jar" "com.example.ExampleDto"

In previous example, `com.example.ExampleDto` is the Scala class for which the TypeScript must be generated.

## Type support

Currently *scala-ts* supports the following types of case class members:

- `Int`, `Double`, `Boolean`, `String`, `Long`
- `List`, `Seq`, `Set`, `Map`
- `Option`, `Either`
- `LocalDate`, `LocalDateTime`, `Instant`, `Timestamp`, `ZonedDateTime`
- `BigDecimal` (mapped to TypeScript's `number`)
- `UUID` (mapped to TypeScript's `string`)
- value classes
- enumeration values
- generic types
- references to other case classes
- (case) objects, as singleton class
- sealed traits, as union type

TODO: Table for mapping between Scala / TS types
