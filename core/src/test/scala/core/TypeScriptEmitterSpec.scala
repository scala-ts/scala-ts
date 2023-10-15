package io.github.scalats.core

import io.github.scalats.ast._
import io.github.scalats.core.Internals.ListSet

final class TypeScriptEmitterSpec
    extends org.specs2.mutable.Specification
    with TypeScriptExtraEmitterSpec {

  "TypeScript emitter".title

  import TranspilerResults._
  import TranspilerCompat.{ ns, valueClassNs }
  import TypeScriptEmitterSpec.emit

  "Emitter" should {
    "emit empty interface" in {
      val empty = InterfaceDeclaration(
        "Empty",
        ListSet.empty,
        typeParams = List.empty,
        superInterface = None,
        union = false
      )

      emit(ListSet(empty)) must beTypedEqualTo("""export interface Empty {
}

export function isEmpty(v: any): v is Empty {
  return (
    typeof v === 'object' && Object.keys(v).length === 0
  );
}
""")
    }

    "emit interface for a class with one primitive member" in {
      emit(ListSet(interface1)) must beTypedEqualTo(
        s"""export interface ${ns}TestClass1 {
  name: string;
}

export function is${ns}TestClass1(v: any): v is ${ns}TestClass1 {
  return (
    ((typeof v['name']) === 'string')
  );
}
"""
      )
    }

    "emit interface for a class with generic member" in {
      emit(ListSet(interface2)) must beTypedEqualTo(
        s"""export interface ${ns}TestClass2<T> {
  name: T;
}

export function is${ns}TestClass2(v: any): v is ${ns}TestClass2 {
  return (
    ((typeof v['name']) === 'T')
  );
}
"""
      )
    }

    "emit interface for a class with generic array" in {
      emit(ListSet(interface3)) must beTypedEqualTo(
        s"""export interface ${ns}TestClass3<T> {
  name: ReadonlyArray<T>;
}

export function is${ns}TestClass3(v: any): v is ${ns}TestClass3 {
  return (
    (Array.isArray(v['name']) && v['name'].every(elmt => (typeof elmt) === 'T'))
  );
}
"""
      )
    }

    "emit interface for a generic case class with a optional member" in {
      emit(ListSet(interface5)) must beTypedEqualTo(
        s"""export interface ${ns}TestClass5<T> {
  name?: T;
  counters: { [key: string]: number };
  time: string;
}

export function is${ns}TestClass5(v: any): v is ${ns}TestClass5 {
  return (
    (!v['name'] || ((typeof v['name']) === 'T')) &&
    ((typeof v['counters']) == 'object' && Object.keys(v['counters']).every(key => ((typeof key) === 'string') && ((typeof v['counters'][key]) === 'number'))) &&
    ((typeof v['time']) === 'string')
  );
}
"""
      )
    }

    "emit interface for a generic case class with disjunction" in {
      // TODO: Add example to documentation
      emit(ListSet(interface7)) must beTypedEqualTo(
        s"""export interface ${ns}TestClass7<T> {
  name: (${ns}TestClass1 | ${ns}TestClass1B);
}

export function is${ns}TestClass7(v: any): v is ${ns}TestClass7 {
  return (
    ((v['name'] && ns${ns}TestClass1.is${ns}TestClass1(v['name'])) || (v['name'] && ns${ns}TestClass1B.is${ns}TestClass1B(v['name'])))
  );
}
"""
      )
    }

    "emit tagged type" >> {
      "as type alias" in {
        emit(ListSet(taggedDeclaration1)) must beTypedEqualTo(
          s"""export type ${valueClassNs}AnyValChild = string;

export function is${valueClassNs}AnyValChild(v: any): v is ${valueClassNs}AnyValChild {
  return (typeof v) === 'string';
}
"""
        )
      }

      "as tagged type" in {
        emit(
          ListSet(taggedDeclaration1),
          declMapper = DeclarationMapper.valueClassAsTagged
        ) must beTypedEqualTo(
          s"""export type ${valueClassNs}AnyValChild = string & { __tag: '${valueClassNs}AnyValChild' };

export function ${valueClassNs}AnyValChild<T extends string>(value: T): ${valueClassNs}AnyValChild & T {
  return value as (${valueClassNs}AnyValChild & T)
}

export function is${valueClassNs}AnyValChild(v: any): v is ${valueClassNs}AnyValChild {
  return (typeof v) === 'string';
}
"""
        )
      }

      "as member" in {
        emit(ListSet(interface8)) must beTypedEqualTo(
          s"""export interface ${valueClassNs}TestClass8 {
  name: ${valueClassNs}AnyValChild;
  aliases: ReadonlyArray<${valueClassNs}AnyValChild>;
}

export function is${valueClassNs}TestClass8(v: any): v is ${valueClassNs}TestClass8 {
  return (
    (v['name'] && ns${valueClassNs}AnyValChild.is${valueClassNs}AnyValChild(v['name'])) &&
    (Array.isArray(v['aliases']) && v['aliases'].every(elmt => elmt && ns${valueClassNs}AnyValChild.is${valueClassNs}AnyValChild(elmt)))
  );
}
"""
        )
      }
    }

    "for singleton" >> {
      "emit class #1" in {
        emit(ListSet(singleton1)) must beTypedEqualTo(
          s"""export class ${ns}TestObject1 {
  private static instance: ${ns}TestObject1;

  private constructor() {}

  public static getInstance() {
    if (!${ns}TestObject1.instance) {
      ${ns}TestObject1.instance = new ${ns}TestObject1();
    }

    return ${ns}TestObject1.instance;
  }
}

export const ${ns}TestObject1Inhabitant: ${ns}TestObject1 = ${ns}TestObject1.getInstance();

export function is${ns}TestObject1(v: any): v is ${ns}TestObject1 {
  return (v instanceof ${ns}TestObject1) && (v === ${ns}TestObject1Inhabitant);
}
"""
        )
      }

      "emit class #2" >> {
        "with value class as constant" in {
          // SCALATS1: No implements SupI
          emit(ListSet(singleton2)) must_=== s"""export class ${ns}TestObject2 implements SupI {
  public name: string & "Foo \\"bar\\"" = "Foo \\"bar\\"";

  public code: number & 1 = 1;

  public const: string & "value" = "value";

  public foo: string = this.name;

  public list: ReadonlyArray<string> = [ "first", this.name ];

  public set: ReadonlySet<number> = new Set([ this.code, 2 ]);

  public readonly mapping: { [key: string]: string } = { "foo": "bar", "lorem": this.name };

  public readonly dictOfList: { [key: string]: ReadonlyArray<string> } = { "excludes": [ "*.txt", ".gitignore" ], "includes": [ "images/**", "*.jpg", "*.png" ] };

  public concatSeq: ReadonlyArray<string> = [ ...this.list, ...[ "foo", "bar" ], ...[ "lorem" ]];

  public concatList: ReadonlyArray<string> = [ ...[ "foo" ], ...this.list];

  public mergedSet: ReadonlySet<number> = new Set([ ...this.set, ...new Set([ 3 ]) ]);

  private static instance: ${ns}TestObject2;

  private constructor() {}

  public static getInstance() {
    if (!${ns}TestObject2.instance) {
      ${ns}TestObject2.instance = new ${ns}TestObject2();
    }

    return ${ns}TestObject2.instance;
  }
}

export const ${ns}TestObject2Inhabitant: ${ns}TestObject2 = ${ns}TestObject2.getInstance();

export function is${ns}TestObject2(v: any): v is ${ns}TestObject2 {
  return (v instanceof ${ns}TestObject2) && (v === ${ns}TestObject2Inhabitant);
}
"""
        }

        "with value clas as dictionary key type" in {
          emit(
            ListSet(singleton3),
            declMapper = DeclarationMapper.valueClassAsTagged
          ) must_=== s"""export class ${valueClassNs}TestObject3 {
  public name: ${valueClassNs}AnyValChild & "Foo" = ns${valueClassNs}AnyValChild.${valueClassNs}AnyValChild("Foo");

  public readonly mapping: { [key: ${valueClassNs}AnyValChild]: string } = (() => { const __buf837556430: { [key: ${valueClassNs}AnyValChild]: string } = {}; __buf837556430[ns${valueClassNs}AnyValChild.${valueClassNs}AnyValChild("foo")] = "bar"; __buf837556430[this.name] = "lorem"; return __buf837556430 })();

  private static instance: ${valueClassNs}TestObject3;

  private constructor() {}

  public static getInstance() {
    if (!${valueClassNs}TestObject3.instance) {
      ${valueClassNs}TestObject3.instance = new ${valueClassNs}TestObject3();
    }

    return ${valueClassNs}TestObject3.instance;
  }
}

export const ${valueClassNs}TestObject3Inhabitant: ${valueClassNs}TestObject3 = ${valueClassNs}TestObject3.getInstance();

export function is${valueClassNs}TestObject3(v: any): v is ${valueClassNs}TestObject3 {
  return (v instanceof ${valueClassNs}TestObject3) && (v === ${valueClassNs}TestObject3Inhabitant);
}
"""

        }

        "with value class as tagged type" in {
          val taggedRef =
            TaggedRef("ScalaRuntimeFixturesAnyValChild", StringRef)

          val singleton2WithTagged = SingletonDeclaration(
            "ScalaRuntimeFixturesTestObject2",
            ListSet(
              LiteralValue("name", StringRef, "\"Foo\""),
              LiteralValue(
                "const",
                taggedRef,
                "\"value\""
              ),
              SelectValue("foo", taggedRef, ThisTypeRef, "const"),
              LiteralValue("code", NumberRef.int, "1"),
              ListValue(
                name = "list",
                typeRef = ArrayRef(taggedRef),
                valueTypeRef = taggedRef,
                elements = List(
                  LiteralValue("list[0]", taggedRef, "\"first\"")
                )
              )
            ),
            superInterface = Some(
              InterfaceDeclaration(
                "SupI",
                ListSet.empty,
                List.empty[String],
                None,
                false
              )
            )
          )

          emit(
            ListSet(singleton2WithTagged),
            declMapper = DeclarationMapper.valueClassAsTagged
          ) must_=== """export class ScalaRuntimeFixturesTestObject2 implements SupI {
  public name: string & "Foo" = "Foo";

  public const: ScalaRuntimeFixturesAnyValChild & "value" = nsScalaRuntimeFixturesAnyValChild.ScalaRuntimeFixturesAnyValChild("value");

  public foo: ScalaRuntimeFixturesAnyValChild = this.const;

  public code: number & 1 = 1;

  public list: ReadonlyArray<ScalaRuntimeFixturesAnyValChild> = [ nsScalaRuntimeFixturesAnyValChild.ScalaRuntimeFixturesAnyValChild("first") ];

  private static instance: ScalaRuntimeFixturesTestObject2;

  private constructor() {}

  public static getInstance() {
    if (!ScalaRuntimeFixturesTestObject2.instance) {
      ScalaRuntimeFixturesTestObject2.instance = new ScalaRuntimeFixturesTestObject2();
    }

    return ScalaRuntimeFixturesTestObject2.instance;
  }
}

export const ScalaRuntimeFixturesTestObject2Inhabitant: ScalaRuntimeFixturesTestObject2 = ScalaRuntimeFixturesTestObject2.getInstance();

export function isScalaRuntimeFixturesTestObject2(v: any): v is ScalaRuntimeFixturesTestObject2 {
  return (v instanceof ScalaRuntimeFixturesTestObject2) && (v === ScalaRuntimeFixturesTestObject2Inhabitant);
}
"""
        }

        "with invariant enum member" in {
          emit(ListSet(singleton4)) must_=== """export class Words {
  public start: ReadonlyArray<Greeting> = [ Greeting.Hello, Greeting.Hi ];

  private static instance: Words;

  private constructor() {}

  public static getInstance() {
    if (!Words.instance) {
      Words.instance = new Words();
    }

    return Words.instance;
  }
}

export const WordsInhabitant: Words = Words.getInstance();

export function isWords(v: any): v is Words {
  return (v instanceof Words) && (v === WordsInhabitant);
}
"""
        }
      }

      "emit class #3" in {
        emit(
          ListSet(unionMember2Singleton)
        ) must_=== s"""export class ${ns}FamilyMember2 implements ${ns}Family {
  public foo: string & "bar" = "bar";

  private static instance: ${ns}FamilyMember2;

  private constructor() {}

  public static getInstance() {
    if (!${ns}FamilyMember2.instance) {
      ${ns}FamilyMember2.instance = new ${ns}FamilyMember2();
    }

    return ${ns}FamilyMember2.instance;
  }
}

export const ${ns}FamilyMember2Inhabitant: ${ns}FamilyMember2 = ${ns}FamilyMember2.getInstance();

export function is${ns}FamilyMember2(v: any): v is ${ns}FamilyMember2 {
  return (v instanceof ${ns}FamilyMember2) && (v === ${ns}FamilyMember2Inhabitant);
}
"""
      }

      "emit literal types" >> {
        val barVal =
          LiteralValue(
            name = "bar",
            typeRef = StringRef,
            rawValue = "\"lorem\""
          )

        "using singleton name" in {
          val obj = SingletonDeclaration(
            name = "Foo",
            values = ListSet.empty,
            superInterface = Option.empty
          )

          emit(
            ListSet(obj),
            declMapper = DeclarationMapper.singletonAsLiteral
          ) must_=== s"""export const FooInhabitant = 'Foo';

export type Foo = typeof FooInhabitant;

export function isFoo(v: any): v is Foo {
  return FooInhabitant == v;
}
"""
        }

        "with string value" in {
          val obj = SingletonDeclaration(
            name = "Foo",
            values = ListSet(barVal),
            superInterface = Option.empty
          )

          emit(
            ListSet(obj),
            declMapper = DeclarationMapper.singletonAsLiteral
          ) must_=== """export const FooInhabitant = "lorem";

export type Foo = typeof FooInhabitant;

export function isFoo(v: any): v is Foo {
  return FooInhabitant == v;
}
"""
        }

        "with object value" in {
          val obj = SingletonDeclaration(
            name = "Foo",
            values = ListSet(
              barVal,
              LiteralValue(
                name = "ipsum",
                typeRef = NumberRef.int,
                rawValue = "2"
              )
            ),
            superInterface = Option.empty
          )

          emit(
            ListSet(obj),
            declMapper = DeclarationMapper.singletonAsLiteral
          ) must_=== """export const FooInhabitant = { bar: "lorem", ipsum: 2 };

export type Foo = typeof FooInhabitant;

export function isFoo(v: any): v is Foo {
  return FooInhabitant == v;
}
"""
        }
      }
    }

    "emit union" >> {
      "as interface" in {
        emit(ListSet(union1)) must_=== s"""export interface ${ns}Family {
  foo: string;
}

export function is${ns}Family(v: any): v is ${ns}Family {
  return (
    ((typeof v['foo']) === 'string')
  );
}
"""
      }

      "as union" in {
        emit(
          ListSet(union1, unionIface),
          declMapper = DeclarationMapper.unionAsSimpleUnion
        ) must_=== s"""export type ${ns}Family = ${ns}FamilyMember1 | ${ns}FamilyMember2 | ${ns}FamilyMember3;

export const ${ns}Family = {
  "bar": ns${ns}FamilyMember2.${ns}FamilyMember2Inhabitant, 
  "lorem": ns${ns}FamilyMember3.${ns}FamilyMember3Inhabitant
} as const;

export function is${ns}Family(v: any): v is ${ns}Family {
  return (
    ns${ns}FamilyMember1.is${ns}FamilyMember1(v) ||
    ns${ns}FamilyMember2.is${ns}FamilyMember2(v) ||
    ns${ns}FamilyMember3.is${ns}FamilyMember3(v)
  );
}
"""
      }
    }

    "emit enumeration" >> {
      "as union" in {
        emit(ListSet(enum1)) must beTypedEqualTo(
          s"""const ${ns}TestEnumerationEntries = {
  A: 'A',
  B: 'B',
  C: 'C',
};

export type ${ns}TestEnumeration = keyof (typeof ${ns}TestEnumerationEntries);

export const ${ns}TestEnumeration = {
  ...${ns}TestEnumerationEntries,
  values: Object.keys(${ns}TestEnumerationEntries)
} as const;

export function is${ns}TestEnumeration(v: any): v is ${ns}TestEnumeration {
  return ${ns}TestEnumeration.values.includes(v);
}
"""
        )
      }

      "as enum" in {
        emit(
          ListSet(enum1),
          declMapper = DeclarationMapper.enumerationAsEnum
        ) must beTypedEqualTo(s"""export enum ${ns}TestEnumeration {
  A = 'A',
  B = 'B',
  C = 'C'
}

export const ${ns}TestEnumerationValues: Array<${ns}TestEnumeration> = [
  ${ns}TestEnumeration.A,
  ${ns}TestEnumeration.B,
  ${ns}TestEnumeration.C
];

export function is${ns}TestEnumeration(v: any): v is ${ns}TestEnumeration {
  return (
    v == 'A' ||
    v == 'B' ||
    v == 'C'
  );
}
""")
      }
    }
  }
}

private[core] object TypeScriptEmitterSpec {

  def emit(
      decls: ListSet[Declaration],
      config: Settings = Settings(),
      declMapper: DeclarationMapper = DeclarationMapper.Defaults,
      importResolver: ImportResolver = ImportResolver.Defaults,
      typeMapper: TypeMapper = TypeMapper.Defaults
    ): String = {
    val buf = new java.io.ByteArrayOutputStream()
    lazy val out = new java.io.PrintStream(buf)

    val emiter = new Emitter(
      config,
      (_, _, _, _) => out,
      importResolver,
      declMapper,
      typeMapper
    )

    try {
      emiter.emit(decls)
      out.flush()
      buf.toString
    } finally {
      out.close()
    }
  }
}
