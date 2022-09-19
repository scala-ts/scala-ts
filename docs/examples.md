---
layout: default
---

# Examples

*Scala-TS* handles various cases and can be configured in many ways.

## Example 1

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

## Example 2

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

> See `optionToNullable` in [configuration](#Configuration) documentation.

## Example 3

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

## Example 4

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
  type: string; // <---- HERE
  messages: Tagged<ReadonlyArray<Message>>;
}

export interface Message {
  format: string;
  language: string;
  text: string;
}
```

> `Locale` type is provided a transpiler as `string`.

In Scala 3, [Opaque Types](https://docs.scala-lang.org/scala3/book/types-opaque-types.html) are supported in a similar way.

```dotty
package scala3ts.examples

object Event {
  opaque type EventType = String
}
```

### Example 4a

The declaration mapped [`valueClassAsTagged`](https://javadoc.io/static/io.github.scala-ts/scala-ts-core_{{site.scala_major_version}}/{{site.last_release}}/io/github/scalats/core/DeclarationMapper$.html#valueClassAsTagged:io.github.scalats.core.DeclarationMapper.ValueClassAsTagged) in [configuration](./config.html) as below.

```ocaml
scalatsDeclarationMappers += valueClassAsTagged
```

Then the Value class `EventType` is generated as a tagged/[branded](https://medium.com/@KevinBGreene/surviving-the-typescript-ecosystem-branding-and-type-tagging-6cf6e516523d) type.

```typescript
export type EventType = string & { __tag: 'EventType' };

// Constructor
export function EventType(value: string): EventType {
  return value as EventType
}

export function isEventType(v: any): v is EventType {
  return (typeof v) === 'string';
}
```

Then such value can be initialized as bellow.

```typescript
const done: EventType = EventType('Done')

isEventType(done) // true
```

## Example 5

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

> See on [GitHub](https://github.com/scala-ts/scala-ts/tree/master/sbt-plugin/src/sbt-test/sbt-scala-ts/simple/)

In Scala 3, [Union Types](https://docs.scala-lang.org/scala3/book/types-union.html) can be used.

```dotty
object Transport:
  type Line = TrainLine | BusLine
```

## Example 6

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

> See `scalatsUnionWithLiteral` SBT settings, [`DeclarationMapper.SingletonAsLiteral`](https://javadoc.io/static/io.github.scala-ts/scala-ts-core_{{site.scala_major_version}}/{{site.latest_release}}/io/github/scalats/core/DeclarationMapper$$SingletonAsLiteral.html) and [`DeclarationMapper.UnionAsSimpleUnion`](https://javadoc.io/static/io.github.scala-ts/scala-ts-core_{{site.scala_major_version}}/{{site.latest_release}}/io/github/scalats/core/DeclarationMapper$$UnionAsSimpleUnion.html) for setting `scalatsDeclarationMappers`, and [`ImportResolver.UnionWithLiteralSingleton`](https://javadoc.io/static/io.github.scala-ts/scala-ts-core_{{site.scala_major_version}}/{{site.latest_release}}/io/github/scalats/core/ImportResolver$$UnionWithLiteralSingleton.html) and `scalatsImportResolvers` setting; Details below in [configuration](#Configuration) documentation.

> See on [GitHub](https://github.com/scala-ts/scala-ts/tree/master/sbt-plugin/src/sbt-test/sbt-scala-ts/enumeratum/) (example with [Enumeratum](https://github.com/lloydmeta/enumeratum#enumeratum------))

## Example 7

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

> See on [GitHub](https://github.com/scala-ts/scala-ts/tree/master/sbt-plugin/src/sbt-test/sbt-scala-ts/single-file-printer/)

In Scala 3, [Enum Types](https://docs.scala-lang.org/scala3/book/types-adts-gadts.html#enumerations) are supported.

```dotty
package scalats.examples

enum WeekDay:
  case Mon
  case Tue
  case Wed
  case Thu
  case Fri
  case Sat
  case Sun
```

## Example 8

Scala [Singleton objects](https://docs.scala-lang.org/tour/singleton-objects.html) as TypeScript [Literal types](https://www.typescriptlang.org/docs/handbook/literal-types.html).

```scala
package scalats.examples

sealed abstract class State(val entryName: String)

case object Alabama extends State("AL")
case object Alaska extends State("AK")
```

*Generated TypeScript:* `entryName` is transpiled as literal.

```typescript
export const AlabamaInhabitant = "AL";

export type Alabama = typeof AlabamaInhabitant;

export const AlaskaInhabitant = "AK";

export type Alaska = typeof AlaskaInhabitant;
```

> See on [GitHub](https://github.com/scala-ts/scala-ts/tree/master/sbt-plugin/src/sbt-test/sbt-scala-ts/enumeratum/)

## Example 9

Stable literal members (`val` or nullary `def`) in single objects as type invariants.

```scala
package scalats.examples

final class Grade(val value: Int) extends AnyVal

object Constants {
  def code = 1
  val name = "foo"
  val LowerGrade = new Grade(0)
}
```

*Generated TypeScript:* `code`, `name` and `LowerGrade` are generated

```typescript
import { Grade, isGrade } from './Grade';

export class Constants {
  public LowerGrade: Grade /* number */ = 0;
  public name: string = "foo";
  public code: number = 1;

  private static instance: Constants;

  private constructor() {}

  public static getInstance() {
    if (!Constants.instance) {
      Constants.instance = new Constants();
    }

    return Constants.instance;
  }
}

export function isConstants(v: any): v is Constants {
  return (v instanceof Constants) && (v === Constants.getInstance());
}
```

> See on [GitHub](https://github.com/scala-ts/scala-ts/tree/master/sbt-plugin/src/sbt-test/sbt-scala-ts/simple/)

### Example 9a

If `valueClassAsTagged` (see [Example 4a](#example-4a)) is applied, then `LowerGrade` is generated as below.

```typescript
public LowerGrade: Grade /* number */ = Grade(0);
```

> See on [GitHub](https://github.com/scala-ts/scala-ts/tree/master/sbt-plugin/src/sbt-test/sbt-scala-ts/custom-cfg/)

### Example 9b

Data structures like `Seq`, `Set` and `Map` are supported, when values are supported.

```scala
package scalats.examples

object ConstantsWithDataStructures {
  def code = 1
  val name = "foo"
  val LowerGrade = new Grade(0)

  val list = List(LowerGrade)
  def set = Set("lorem", "ipsum")

  val dict = Map(
    "A" -> "value #1",
    "B" -> name)
}
```

*Generated TypeScript:* `list`, `set` and `dict` are generated as `ReadonlyArray`, `ReadonlySet` and `object`.

> Note that the non-literal stable terms as `LowerGrade` in `list` are supported.

```typescript
import { Grade, isGrade } from './Grade';

export class Constants {
  public code: number = 1;

  public name: string = "foo";

  public LowerGrade: Grade = 0;

  public list: ReadonlyArray<Grade> = [ this.LowerGrade ];

  public set: ReadonlySet<string> = new Set("lorem", "ipsum");

  public readonly dict = {
    'A': "value #1",
    'B': this.name
  }
}
```