package io.github.scalats.core

import scala.collection.immutable.ListSet

import io.github.scalats.typescript._

final class TypeScriptEmitterSpec extends org.specs2.mutable.Specification {
  "TypeScript emitter" title

  import TranspilerResults._

  "Emitter" should {
    "emit empty interface" in {
      val empty = InterfaceDeclaration(
        "Empty", ListSet.empty,
        typeParams = List.empty,
        superInterface = None,
        union = false)

      emit(ListSet(empty)) must beTypedEqualTo("""export interface Empty {
}

export function isEmpty(v: any): v is Empty {
  return (
    v === {}
  );
}
""")
    }

    "emit interface for a class with one primitive member" in {
      emit(ListSet(interface1)) must beTypedEqualTo("""export interface ScalaRuntimeFixturesTestClass1 {
  name: string;
}

export function isScalaRuntimeFixturesTestClass1(v: any): v is ScalaRuntimeFixturesTestClass1 {
  return (
    ((typeof v['name']) === 'string')
  );
}
""")
    }

    "emit interface for a class with generic member" in {
      emit(ListSet(interface2)) must beTypedEqualTo("""export interface ScalaRuntimeFixturesTestClass2<T> {
  name: T;
}

export function isScalaRuntimeFixturesTestClass2(v: any): v is ScalaRuntimeFixturesTestClass2 {
  return (
    ((typeof v['name']) === 'T')
  );
}
""")
    }

    "emit interface for a class with generic array" in {
      emit(ListSet(interface3)) must beTypedEqualTo("""export interface ScalaRuntimeFixturesTestClass3<T> {
  name: ReadonlyArray<T>;
}

export function isScalaRuntimeFixturesTestClass3(v: any): v is ScalaRuntimeFixturesTestClass3 {
  return (
    (Array.isArray(v['name']) && v['name'].every(elmt => (typeof elmt) === 'T'))
  );
}
""")
    }

    "emit interface for a generic case class with a optional member" in {
      emit(ListSet(interface5)) must beTypedEqualTo("""export interface ScalaRuntimeFixturesTestClass5<T> {
  name?: T;
  counters: { [key: string]: number };
}

export function isScalaRuntimeFixturesTestClass5(v: any): v is ScalaRuntimeFixturesTestClass5 {
  return (
    ((typeof v['counters']) == 'object' && Object.keys(v['counters']).every(key => ((typeof key) === 'string') && ((typeof v['counters'][key]) === 'number'))) &&
    (!v['name'] || ((typeof v['name']) === 'T'))
  );
}
""")
    }

    "emit interface for a generic case class with disjunction" in {
      // TODO: Add example to documentation
      emit(ListSet(interface7)) must beTypedEqualTo("""export interface ScalaRuntimeFixturesTestClass7<T> {
  name: (ScalaRuntimeFixturesTestClass1 | ScalaRuntimeFixturesTestClass1B);
}

export function isScalaRuntimeFixturesTestClass7(v: any): v is ScalaRuntimeFixturesTestClass7 {
  return (
    ((v['name'] && isScalaRuntimeFixturesTestClass1(v['name'])) || (v['name'] && isScalaRuntimeFixturesTestClass1B(v['name'])))
  );
}
""")
    }

    "for singleton" >> {
      "emit class #1" in {
        emit(ListSet(singleton1)) must beTypedEqualTo("""export class ScalaRuntimeFixturesTestObject1 {
  private static instance: ScalaRuntimeFixturesTestObject1;

  private constructor() {}

  public static getInstance() {
    if (!ScalaRuntimeFixturesTestObject1.instance) {
      ScalaRuntimeFixturesTestObject1.instance = new ScalaRuntimeFixturesTestObject1();
    }

    return ScalaRuntimeFixturesTestObject1.instance;
  }
}

export function isScalaRuntimeFixturesTestObject1(v: any): v is ScalaRuntimeFixturesTestObject1 {
  return (v instanceof ScalaRuntimeFixturesTestObject1) && (v === ScalaRuntimeFixturesTestObject1.getInstance());
}
""")
      }

      "emit class #2" in {
        // SCALATS1: No implements SupI
        emit(ListSet(singleton2)) must_=== """export class ScalaRuntimeFixturesTestObject2 implements SupI {
  public name: string = "Foo";
  public code: number = 1;

  private static instance: ScalaRuntimeFixturesTestObject2;

  private constructor() {}

  public static getInstance() {
    if (!ScalaRuntimeFixturesTestObject2.instance) {
      ScalaRuntimeFixturesTestObject2.instance = new ScalaRuntimeFixturesTestObject2();
    }

    return ScalaRuntimeFixturesTestObject2.instance;
  }
}

export function isScalaRuntimeFixturesTestObject2(v: any): v is ScalaRuntimeFixturesTestObject2 {
  return (v instanceof ScalaRuntimeFixturesTestObject2) && (v === ScalaRuntimeFixturesTestObject2.getInstance());
}
"""
      }

      "emit class #3" in {
        emit(ListSet(unionMember2Singleton)) must_=== """export class ScalaRuntimeFixturesFamilyMember2 implements ScalaRuntimeFixturesFamily {
  public foo: string = "bar";

  private static instance: ScalaRuntimeFixturesFamilyMember2;

  private constructor() {}

  public static getInstance() {
    if (!ScalaRuntimeFixturesFamilyMember2.instance) {
      ScalaRuntimeFixturesFamilyMember2.instance = new ScalaRuntimeFixturesFamilyMember2();
    }

    return ScalaRuntimeFixturesFamilyMember2.instance;
  }
}

export function isScalaRuntimeFixturesFamilyMember2(v: any): v is ScalaRuntimeFixturesFamilyMember2 {
  return (v instanceof ScalaRuntimeFixturesFamilyMember2) && (v === ScalaRuntimeFixturesFamilyMember2.getInstance());
}
"""
      }

      "emit literal types" >> {
        val barVal = Value(
          name = "bar",
          typeRef = StringRef,
          rawValue = "\"lorem\"")

        "using singleton name" in {
          val obj = SingletonDeclaration(
            name = "Foo",
            values = ListSet.empty,
            superInterface = Option.empty)

          emit(
            ListSet(obj),
            declMapper = TypeScriptDeclarationMapper.singletonAsLiteral) must_=== """export const FooInhabitant = 'Foo';

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
            superInterface = Option.empty)

          emit(
            ListSet(obj),
            declMapper = TypeScriptDeclarationMapper.singletonAsLiteral) must_=== """export const FooInhabitant = "lorem";

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
              Value(
                name = "ipsum",
                typeRef = NumberRef,
                rawValue = "2")),
            superInterface = Option.empty)

          emit(
            ListSet(obj),
            declMapper = TypeScriptDeclarationMapper.singletonAsLiteral) must_=== """export const FooInhabitant = { bar: "lorem", ipsum: 2 };

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
        emit(ListSet(union1)) must_=== """export interface ScalaRuntimeFixturesFamily {
  foo: string;
}

export function isScalaRuntimeFixturesFamily(v: any): v is ScalaRuntimeFixturesFamily {
  return (
    ((typeof v['foo']) === 'string')
  );
}
"""
      }

      "as union" in {
        emit(
          ListSet(union1, unionIface),
          declMapper = TypeScriptDeclarationMapper.unionAsSimpleUnion) must_=== """export type ScalaRuntimeFixturesFamily = ScalaRuntimeFixturesFamilyMember1 | ScalaRuntimeFixturesFamilyMember2 | ScalaRuntimeFixturesFamilyMember3;

export function isScalaRuntimeFixturesFamily(v: any): v is ScalaRuntimeFixturesFamily {
  return (
    isScalaRuntimeFixturesFamilyMember1(v) ||
    isScalaRuntimeFixturesFamilyMember2(v) ||
    isScalaRuntimeFixturesFamilyMember3(v)
  );
}
"""
      }
    }

    "emit enumeration" >> {
      "as union" in {
        emit(ListSet(enum1)) must beTypedEqualTo("""export type ScalaRuntimeFixturesTestEnumeration = 'A' | 'B' | 'C';

export const ScalaRuntimeFixturesTestEnumerationValues = [ 'A', 'B', 'C' ];

export function isScalaRuntimeFixturesTestEnumeration(v: any): v is ScalaRuntimeFixturesTestEnumeration {
  return (
    v == 'A' ||
    v == 'B' ||
    v == 'C'
  );
}
""")
      }

      "as enum" in {
        emit(ListSet(enum1), declMapper = TypeScriptDeclarationMapper.enumerationAsEnum) must beTypedEqualTo("""export enum ScalaRuntimeFixturesTestEnumeration {
  A = 'A',
  B = 'B',
  C = 'C'
}

export const ScalaRuntimeFixturesTestEnumerationValues: Array<ScalaRuntimeFixturesTestEnumeration> = [
  ScalaRuntimeFixturesTestEnumeration.A,
  ScalaRuntimeFixturesTestEnumeration.B,
  ScalaRuntimeFixturesTestEnumeration.C
];

export function isScalaRuntimeFixturesTestEnumeration(v: any): v is ScalaRuntimeFixturesTestEnumeration {
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

  // ---

  private lazy val defaultConfig = Settings()

  def emit(
    decls: ListSet[Declaration],
    config: Settings = defaultConfig,
    declMapper: TypeScriptDeclarationMapper = TypeScriptDeclarationMapper.Defaults,
    importResolver: TypeScriptImportResolver = TypeScriptImportResolver.Defaults,
    typeMapper: TypeScriptTypeMapper = TypeScriptTypeMapper.Defaults): String = {
    val buf = new java.io.ByteArrayOutputStream()
    lazy val out = new java.io.PrintStream(buf)

    val emiter = new TypeScriptEmitter(
      config, (_, _, _, _) => out,
      importResolver,
      declMapper,
      typeMapper)

    try {
      emiter.emit(decls)
      out.flush()
      buf.toString
    } finally {
      out.close()
    }
  }
}
