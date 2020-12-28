package io.github.scalats.core

import scala.collection.immutable.ListSet

import io.github.scalats.typescript._

final class TypeScriptEmitterSpec extends org.specs2.mutable.Specification {
  "TypeScript emitter" title

  import TranspilerResults._

  "Emitter" should {
    "emit interface for a class with one primitive member" in {
      emit(ListSet(interface1)) must beTypedEqualTo("""export interface ScalaRuntimeFixturesTestClass1 {
  name: string;
}
""")
    }

    "emit interface for a class with generic member" in {
      emit(ListSet(interface2)) must beTypedEqualTo("""export interface ScalaRuntimeFixturesTestClass2<T> {
  name: T;
}
""")
    }

    "emit interface for a class with generic array" in {
      emit(ListSet(interface3)) must beTypedEqualTo("""export interface ScalaRuntimeFixturesTestClass3<T> {
  name: ReadonlyArray<T>;
}
""")
    }

    "emit interface for a generic case class with a optional member" in {
      emit(ListSet(interface5)) must beTypedEqualTo("""export interface ScalaRuntimeFixturesTestClass5<T> {
  name?: T;
  counters: { [key: string]: number };
}
""")
    }

    "emit interface for a generic case class with disjunction" in {
      // TODO: Add example to documentation
      emit(ListSet(interface7)) must beTypedEqualTo("""export interface ScalaRuntimeFixturesTestClass7<T> {
  name: (ScalaRuntimeFixturesTestClass1 | ScalaRuntimeFixturesTestClass1B);
}
""")
    }

    "emit class for a singleton #1" in {
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
""")
    }

    "emit class for a singleton #2" in {
      // SCALATS1: No implements SupI
      emit(ListSet(singleton2)) must beTypedEqualTo("""export class ScalaRuntimeFixturesTestObject2 implements SupI {
  private static instance: ScalaRuntimeFixturesTestObject2;

  private constructor() {}

  public static getInstance() {
    if (!ScalaRuntimeFixturesTestObject2.instance) {
      ScalaRuntimeFixturesTestObject2.instance = new ScalaRuntimeFixturesTestObject2();
    }

    return ScalaRuntimeFixturesTestObject2.instance;
  }
}
""")
    }

    "emit singleton as union member #2" in {
      emit(ListSet(unionMember2Singleton)) must startWith("// WARNING: Cannot emit static members for properties of singleton 'ScalaRuntimeFixturesFamilyMember2': foo (string)")
    }

    "emit union" in {
      emit(ListSet(union1)) must beTypedEqualTo("""export interface ScalaRuntimeFixturesFamily {
  foo: string;
}
""")
    }

    "emit enumeration" >> {
      "as union" in {
        emit(ListSet(enum1)) must beTypedEqualTo("""export type ScalaRuntimeFixturesTestEnumeration = 'A' | 'B' | 'C'

export const ScalaRuntimeFixturesTestEnumerationValues = [ 'A', 'B', 'C' ]
""")
      }

      "as enum" in {
        emit(ListSet(enum1), declMapper = TypeScriptDeclarationMapper.enumerationAsEnum) must beTypedEqualTo("""export enum ScalaRuntimeFixturesTestEnumeration {
  A = 'A',
  B = 'B',
  C = 'C'
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
    typeMapper: TypeScriptTypeMapper = TypeScriptTypeMapper.Defaults): String = {
    val buf = new java.io.ByteArrayOutputStream()
    lazy val out = new java.io.PrintStream(buf)

    val emiter = new TypeScriptEmitter(
      config, (_, _, _, _) => out,
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
