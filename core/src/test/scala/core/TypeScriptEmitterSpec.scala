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

      emit(Map("Empty" -> ListSet(empty))) must beTypedEqualTo(
        """export interface Empty {
}

export function isEmpty(v: any): v is Empty {
  return (
    typeof v === 'object' && Object.keys(v).length === 0
  );
}
"""
      )
    }

    "emit interface for a class with one primitive member" in {
      emit(
        Map(interface1.name -> ListSet(interface1))
      ) must beTypedEqualTo(
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
      emit(Map(interface2.name -> ListSet(interface2))) must beTypedEqualTo(
        s"""export interface ${ns}TestClass2<T> {
  name: T;
}

// No valid type guard for generic interface ${ns}TestClass2
"""
      )
    }

    "emit interface for a class with generic array" in {
      emit(Map(interface3.name -> ListSet(interface3))) must beTypedEqualTo(
        s"""export interface ${ns}TestClass3<T> {
  name: readonly [T, ...ReadonlyArray<T>];
}

// No valid type guard for generic interface ${ns}TestClass3
"""
      )
    }

    "emit interface for a generic case class with a optional member" in {
      emit(Map(interface5.name -> ListSet(interface5))) must beTypedEqualTo(
        s"""export interface ${ns}TestClass5<T> {
  name?: T;
  counters: Readonly<Map<string, number>>;
  time: string;
}

// No valid type guard for generic interface ${ns}TestClass5
"""
      )
    }

    "emit interface for a generic case class with disjunction" in {
      // TODO: Add example to documentation
      emit(Map(interface7.name -> ListSet(interface7))) must beTypedEqualTo(
        s"""export interface ${ns}TestClass7<T> {
  name: (${ns}TestClass1 | ${ns}TestClass1B);
}

// No valid type guard for generic interface ${ns}TestClass7
"""
      )
    }

    "emit tagged type" >> {
      "as type alias" in {
        emit(
          Map(taggedDeclaration1.name -> ListSet(taggedDeclaration1))
        ) must beTypedEqualTo(
          s"""export type ${valueClassNs}AnyValChild = string;

export function is${valueClassNs}AnyValChild(v: any): v is ${valueClassNs}AnyValChild {
  return (typeof v) === 'string';
}
"""
        )
      }

      "as tagged type" in {
        emit(
          Map(taggedDeclaration1.name -> ListSet(taggedDeclaration1)),
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
        emit(Map(interface8.name -> ListSet(interface8))) must beTypedEqualTo(
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
        emit(Map(singleton1.name -> ListSet(singleton1))) must beTypedEqualTo(
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

export type ${ns}TestObject1Singleton = ${ns}TestObject1;
"""
        )
      }

      "emit class #2" >> {
        "with value class as constant" in {
          // SCALATS1: No implements SupI
          emit(
            Map(singleton2.name -> ListSet(singleton2))
          ) must_=== s"""export class ${ns}TestObject2 implements SupI {
  public readonly name: string & "Foo \\"bar\\"" = "Foo \\"bar\\"";

  public readonly code: number & 1 = 1;

  public readonly const: string & "value" = "value";

  public readonly foo: string = this.name;

  public readonly list: readonly [string, ...ReadonlyArray<string>] = [ "first", this.name ];

  public readonly set: ReadonlySet<number> = new Set([ this.code, 2 ]);

  public readonly mapping: Readonly<Map<string, string>> = new Map([ ["foo", "bar"], ["lorem", this.name] ]);

  public readonly dictOfList: Readonly<Map<string, ReadonlyArray<string>>> = new Map([ ["excludes", [ "*.txt", ".gitignore" ]], ["includes", [ "images/**", "*.jpg", "*.png" ]] ]);

  public readonly concatSeq: ReadonlyArray<string> = [ ...this.list, ...[ "foo", "bar" ], ...[ "lorem" ]];

  public readonly concatList: ReadonlyArray<string> = [ ...[ "foo" ], ...this.list];

  public readonly mergedSet: ReadonlySet<number> = new Set([ ...this.set, ...new Set([ 3 ]) ]);

  public readonly tuple1: Readonly<[string, number, number]> = [ "foo", 2, 3.0 ];

  public readonly tuple2: Readonly<[string, number]> = [ "bar", 2 ];

  public readonly tuple3: [string, number, number] = this.tuple1;

  public readonly tuple4: Readonly<[string, number, number]> = [ "lorem", 10, 20 ];

  public readonly Nested1: ns${ns}TestObject2Nested1.${ns}TestObject2Nested1Singleton = ns${ns}TestObject2Nested1.${ns}TestObject2Nested1Inhabitant;

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

export type ${ns}TestObject2Singleton = ${ns}TestObject2;
"""
        }

        "with value class as dictionary key type" in {
          emit(
            Map(singleton3.name -> ListSet(singleton3)),
            declMapper = DeclarationMapper.valueClassAsTagged
          ) must_=== s"""export class ${valueClassNs}TestObject3 {
  public readonly name: ${valueClassNs}AnyValChild & "Foo" = ns${valueClassNs}AnyValChild.${valueClassNs}AnyValChild("Foo");

  public readonly mapping: Readonly<Map<${valueClassNs}AnyValChild, string>> = (() => { const __buf837556430: Map<${valueClassNs}AnyValChild, string> = new Map(); __buf837556430.set(ns${valueClassNs}AnyValChild.${valueClassNs}AnyValChild("foo"), "bar"); __buf837556430.set(this.name, "lorem"); return __buf837556430 })();

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

export type ${valueClassNs}TestObject3Singleton = ${valueClassNs}TestObject3;
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
                typeRef = ArrayRef(taggedRef, false),
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
            Map(singleton2WithTagged.name -> ListSet(singleton2WithTagged)),
            declMapper = DeclarationMapper.valueClassAsTagged
          ) must_=== """export class ScalaRuntimeFixturesTestObject2 implements SupI {
  public readonly name: string & "Foo" = "Foo";

  public readonly const: ScalaRuntimeFixturesAnyValChild & "value" = nsScalaRuntimeFixturesAnyValChild.ScalaRuntimeFixturesAnyValChild("value");

  public readonly foo: ScalaRuntimeFixturesAnyValChild = this.const;

  public readonly code: number & 1 = 1;

  public readonly list: ReadonlyArray<ScalaRuntimeFixturesAnyValChild> = [ nsScalaRuntimeFixturesAnyValChild.ScalaRuntimeFixturesAnyValChild("first") ];

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

export type ScalaRuntimeFixturesTestObject2Singleton = ScalaRuntimeFixturesTestObject2;
"""
        }

        "with invariant enum member" in {
          emit(
            Map(singleton4.name -> ListSet(singleton4))
          ) must_=== """export class Words {
  public readonly start: ReadonlyArray<Greeting> = [ Greeting.Hello, Greeting.Hi ];

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

export type WordsSingleton = Words;
"""
        }
      }

      "emit class #3" in {
        emit(
          Map(unionMember2Singleton.name -> ListSet(unionMember2Singleton))
        ) must_=== s"""export class ${ns}FamilyMember2 implements ${ns}Family {
  public readonly foo: string & "bar" = "bar";

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

export type ${ns}FamilyMember2Singleton = ${ns}FamilyMember2;
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
            Map(obj.name -> ListSet(obj)),
            declMapper = DeclarationMapper.singletonAsLiteral
          ) must_=== s"""export const FooInhabitant = 'Foo';

export type Foo = typeof FooInhabitant;

export function isFoo(v: any): v is Foo {
  return FooInhabitant == v;
}

export type FooSingleton = Foo;
"""
        }

        "with string value" in {
          val obj = SingletonDeclaration(
            name = "Foo",
            values = ListSet(barVal),
            superInterface = Option.empty
          )

          emit(
            Map(obj.name -> ListSet(obj)),
            declMapper = DeclarationMapper.singletonAsLiteral
          ) must_=== """export const FooInhabitant = "lorem";

export type Foo = typeof FooInhabitant;

export function isFoo(v: any): v is Foo {
  return FooInhabitant == v;
}

export type FooSingleton = Foo;
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
            Map(obj.name -> ListSet(obj)),
            declMapper = DeclarationMapper.singletonAsLiteral
          ) must_=== """export const FooInhabitant = { bar: "lorem", ipsum: 2 };

export type Foo = typeof FooInhabitant;

export function isFoo(v: any): v is Foo {
  return FooInhabitant == v;
}

export type FooSingleton = Foo;

class FooValuesClass {
  public readonly bar: string & "lorem" = "lorem";
  public readonly ipsum: number & 2 = 2;
}

export const FooValues = new FooValuesClass();
"""
        }
      }
    }

    "emit union" >> {
      "as interface" in {
        emit(
          Map(union1.name -> ListSet(union1))
        ) must_=== s"""export interface ${ns}Family {
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
          Map("Family" -> ListSet(union1, unionIface)),
          declMapper = DeclarationMapper.unionAsSimpleUnion
        ) must_=== s"""export type ${ns}FamilyUnion = ${ns}FamilyMember1 | ns${ns}FamilyMember2.${ns}FamilyMember2 | ns${ns}FamilyMember3.${ns}FamilyMember3;

export const ${ns}FamilyUnion = {
  "bar": ns${ns}FamilyMember2.${ns}FamilyMember2Inhabitant, 
  "lorem": ns${ns}FamilyMember3.${ns}FamilyMember3Inhabitant
} as const;

export function is${ns}FamilyUnion(v: any): v is ${ns}FamilyUnion {
  return (
    ns${ns}FamilyMember1.is${ns}FamilyMember1(v) ||
    ns${ns}FamilyMember2.is${ns}FamilyMember2(v) ||
    ns${ns}FamilyMember3.is${ns}FamilyMember3(v)
  );
}

export interface I${ns}Family {
  foo: string;
}

export function isI${ns}Family(v: any): v is I${ns}Family {
  return (
    ((typeof v['foo']) === 'string')
  );
}

export type Family = ${ns}FamilyUnion & I${ns}Family;

export function isFamily(v: any): v is Family {
  return (
    is${ns}FamilyUnion(v) &&
    isI${ns}Family(v)
  );
}"""
      }
    }

    "emit enumeration" >> {
      "as union" in {
        emit(Map(enum1.name -> ListSet(enum1))) must beTypedEqualTo(
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
          Map(enum1.name -> ListSet(enum1)),
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

    "emit composite" >> {
      "with empty singleton" in {
        emit(
          Map(singleton1.name -> ListSet(singleton1, union1))
        ) must_=== s"""export interface ${ns}Family {
  foo: string;
}

export function is${ns}Family(v: any): v is ${ns}Family {
  return (
    ((typeof v['foo']) === 'string')
  );
}
"""
      }

      "with non-empty singleton" in {
        emit(
          Map(singleton4.name -> ListSet(singleton4, union1))
        ) must_=== s"""export class WordsSingleton {
  public readonly start: ReadonlyArray<Greeting> = [ Greeting.Hello, Greeting.Hi ];

  private static instance: WordsSingleton;

  private constructor() {}

  public static getInstance() {
    if (!WordsSingleton.instance) {
      WordsSingleton.instance = new WordsSingleton();
    }

    return WordsSingleton.instance;
  }
}

export const WordsSingletonInhabitant: WordsSingleton = WordsSingleton.getInstance();

export function isWordsSingleton(v: any): v is WordsSingleton {
  return (v instanceof WordsSingleton) && (v === WordsSingletonInhabitant);
}

export const WordsInhabitant = WordsSingletonInhabitant;

export interface ${ns}FamilyUnion {
  foo: string;
}

export function is${ns}FamilyUnion(v: any): v is ${ns}FamilyUnion {
  return (
    ((typeof v['foo']) === 'string')
  );
}

export type Words = ${ns}FamilyUnion;

export function isWords(v: any): v is Words {
  return (
    is${ns}FamilyUnion(v)
  );
}"""
      }
    }

    "emit value body" >> {
      "when Set" in {
        val (emitter, ps, buf) = TypeScriptEmitterSpec.emitter()
        val setValue = SetValue(
          name = "foo",
          typeRef = SetRef(StringRef),
          valueTypeRef = StringRef,
          elements = Set(
            LiteralValue("foo[0]", StringRef, "\"lorem\""),
            LiteralValue("foo[1]", StringRef, "\"bar\""),
            LiteralValue("foo[2]", StringRef, "\"alpha\""),
            LiteralValue("foo[3]", StringRef, "\"dolor\""),
            LiteralValue("foo[4]", StringRef, "\"huit\"")
          )
        )

        val memberDecl = new ValueMemberDeclaration(
          SingletonDeclaration("Test", ListSet.empty, None),
          setValue
        )

        emitter.emitValueBody(
          new ValueBodyDeclaration(memberDecl, setValue),
          Map.empty,
          ps
        )

        val ls = setValue.elements.toList.collect {
          case LiteralValue(_, _, v) => v
        }.mkString(", ")

        buf.toString must_=== s"""new Set([ ${ls} ])"""
      }
    }
  }
}

private[core] object TypeScriptEmitterSpec {
  import java.io.{ ByteArrayOutputStream, PrintStream }

  def emitter(
      config: Settings = Settings(),
      declMapper: DeclarationMapper = DeclarationMapper.Defaults,
      importResolver: ImportResolver = ImportResolver.Defaults,
      typeMapper: TypeMapper = TypeMapper.Defaults
    ): (Emitter, PrintStream, ByteArrayOutputStream) = {
    val buf = new ByteArrayOutputStream()
    lazy val out = new PrintStream(buf)

    Tuple3(
      new Emitter(
        config,
        (_, _, _, _, _) => out,
        importResolver,
        declMapper,
        typeMapper
      ),
      out,
      buf
    )
  }

  def emit(
      decls: Map[String, ListSet[Declaration]],
      config: Settings = Settings(),
      declMapper: DeclarationMapper = DeclarationMapper.Defaults,
      importResolver: ImportResolver = ImportResolver.Defaults,
      typeMapper: TypeMapper = TypeMapper.Defaults
    ): String = {
    val (em, out, buf) =
      emitter(config, declMapper, importResolver, typeMapper)

    try {
      em.emit(decls)
      out.flush()
      buf.toString
    } finally {
      out.close()
    }
  }
}
