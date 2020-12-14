package io.github.scalats.core

import scala.collection.immutable.ListSet

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

final class TypeScriptEmitterSpec extends AnyFlatSpec with Matchers {
  import TypeScriptModel._
  import TranspilerResults._

  it should "emit class with one primitive member" in {
    emit(ListSet(clazz1)) should equal("""export class ScalaRuntimeFixturesTestClass1 implements IScalaRuntimeFixturesTestClass1 {
  constructor(
    public name: string
  ) {
    this.name = name;
  }

  public static fromData(data: any): ScalaRuntimeFixturesTestClass1 {
    return <ScalaRuntimeFixturesTestClass1>(data);
  }

  public static toData(instance: ScalaRuntimeFixturesTestClass1): any {
    return instance;
  }
}
""")
  }

  it should "emit interface for a class with one primitive member" in {
    emit(ListSet(interface1)) should equal("""export interface IScalaRuntimeFixturesTestClass1 {
  name: string;
}
""")
  }

  it should "emit class with generic member" in {
    emit(ListSet(clazz2)) should equal("""export class ScalaRuntimeFixturesTestClass2<T> implements IScalaRuntimeFixturesTestClass2<T> {
  constructor(
    public name: T
  ) {
    this.name = name;
  }

  public static fromData<T>(data: any): ScalaRuntimeFixturesTestClass2<T> {
    return <ScalaRuntimeFixturesTestClass2<T>>(data);
  }

  public static toData<T>(instance: ScalaRuntimeFixturesTestClass2<T>): any {
    return instance;
  }
}
""")
  }

  it should "emit interface for a class with generic member" in {
    emit(ListSet(interface2)) should equal("""export interface IScalaRuntimeFixturesTestClass2<T> {
  name: T;
}
""")
  }

  it should "emit class with generic array" in {
    emit(ListSet(clazz3)) should equal("""export class ScalaRuntimeFixturesTestClass3<T> implements IScalaRuntimeFixturesTestClass3<T> {
  constructor(
    public name: T[]
  ) {
    this.name = name;
  }

  public static fromData<T>(data: any): ScalaRuntimeFixturesTestClass3<T> {
    return <ScalaRuntimeFixturesTestClass3<T>>(data);
  }

  public static toData<T>(instance: ScalaRuntimeFixturesTestClass3<T>): any {
    return instance;
  }
}
""")
  }

  it should "emit interface for a class with generic array" in {
    emit(ListSet(interface3)) should equal("""export interface IScalaRuntimeFixturesTestClass3<T> {
  name: T[];
}
""")
  }

  it should "emit class for a generic case class with one optional member" in {
    emit(ListSet(clazz5)) should equal("""export class ScalaRuntimeFixturesTestClass5<T> implements IScalaRuntimeFixturesTestClass5<T> {
  constructor(
    public name: (T | null)
  ) {
    this.name = name;
  }

  public static fromData<T>(data: any): ScalaRuntimeFixturesTestClass5<T> {
    return <ScalaRuntimeFixturesTestClass5<T>>(data);
  }

  public static toData<T>(instance: ScalaRuntimeFixturesTestClass5<T>): any {
    return instance;
  }
}
""")
  }

  it should "emit interface for a generic case class with one optional member" in {
    emit(ListSet(interface5)) should equal("""export interface IScalaRuntimeFixturesTestClass5<T> {
  name: (T | null);
}
""")
  }

  it should "emit generic case class with disjunction" in {
    emit(ListSet(clazz7)) should equal("""export class ScalaRuntimeFixturesTestClass7<T> implements IScalaRuntimeFixturesTestClass7<T> {
  constructor(
    public name: (ScalaRuntimeFixturesTestClass1 | ScalaRuntimeFixturesTestClass1B)
  ) {
    this.name = name;
  }

  public static fromData<T>(data: any): ScalaRuntimeFixturesTestClass7<T> {
    return <ScalaRuntimeFixturesTestClass7<T>>(data);
  }

  public static toData<T>(instance: ScalaRuntimeFixturesTestClass7<T>): any {
    return instance;
  }
}
""")
  }

  it should "emit generic case class with tuple values" in {
    emit(ListSet(clazz10)) should equal("""export class ScalaRuntimeFixturesTestClass10 implements IScalaRuntimeFixturesTestClass10 {
  constructor(
    public tupleC: [string, string, number],
    public tupleB: [string, number],
    public tupleA: [string, number],
    public tuple: [number],
    public name: string
  ) {
    this.tupleC = tupleC;
    this.tupleB = tupleB;
    this.tupleA = tupleA;
    this.tuple = tuple;
    this.name = name;
  }

  public static fromData(data: any): ScalaRuntimeFixturesTestClass10 {
    return <ScalaRuntimeFixturesTestClass10>(data);
  }

  public static toData(instance: ScalaRuntimeFixturesTestClass10): any {
    return instance;
  }
}
""")
  }

  it should "emit interface for a generic case class with disjunction" in {
    emit(ListSet(interface7)) should equal("""export interface IScalaRuntimeFixturesTestClass7<T> {
  name: (IScalaRuntimeFixturesTestClass1 | IScalaRuntimeFixturesTestClass1B);
}
""")
  }

  it should "emit class using FieldNaming.SnakeCase" in {
    val clazz = ClassDeclaration("Test", ClassConstructor(ListSet(
      ClassConstructorParameter("name", SimpleTypeRef("T")),
      ClassConstructorParameter("fooBar", TypeScriptModel.StringRef))),
      ListSet.empty,
      List("T"), Option.empty)

    val config = defaultConfig.copy(fieldNaming = FieldNaming.SnakeCase)

    emit(ListSet(clazz), config) should equal("""export class Test<T> implements ITest<T> {
  constructor(
    public name: T,
    public foo_bar: string
  ) {
    this.name = name;
    this.foo_bar = foo_bar;
  }

  public static fromData<T>(data: any): Test<T> {
    return new Test<T>(data.name, data.foo_bar);
  }

  public static toData<T>(instance: Test<T>): any {
    return {
      name: instance.name,
      foo_bar: instance.foo_bar
    };
  }
}
""")
  }

  it should "emit class for a singleton #1" in {
    emit(ListSet(singleton1)) should equal("""export class ScalaRuntimeFixturesTestObject1 {
  private static instance: ScalaRuntimeFixturesTestObject1;

  private constructor() {}

  public static getInstance() {
    if (!ScalaRuntimeFixturesTestObject1.instance) {
      ScalaRuntimeFixturesTestObject1.instance = new ScalaRuntimeFixturesTestObject1();
    }

    return ScalaRuntimeFixturesTestObject1.instance;
  }

  public static fromData(data: any): ScalaRuntimeFixturesTestObject1 {
    return ScalaRuntimeFixturesTestObject1.instance;
  }

  public static toData(instance: ScalaRuntimeFixturesTestObject1): any {
    return instance;
  }
}
""")
  }

  it should "emit class for a singleton #2" in {
    // SCALATS1: No implements SupI
    emit(ListSet(singleton2)) should equal("""export class ScalaRuntimeFixturesTestObject2 implements SupI {
  private static instance: ScalaRuntimeFixturesTestObject2;

  private constructor() {}

  public static getInstance() {
    if (!ScalaRuntimeFixturesTestObject2.instance) {
      ScalaRuntimeFixturesTestObject2.instance = new ScalaRuntimeFixturesTestObject2();
    }

    return ScalaRuntimeFixturesTestObject2.instance;
  }

  public static fromData(data: any): ScalaRuntimeFixturesTestObject2 {
    return ScalaRuntimeFixturesTestObject2.instance;
  }

  public static toData(instance: ScalaRuntimeFixturesTestObject2): any {
    return instance;
  }
}
""")
  }

  it should "emit class as union member #1" in {
    the[IllegalStateException].
      thrownBy(emit(ListSet(unionMember1Clazz))) should have message (
        "Cannot emit static members for class values: code (number)")

  }

  it should "emit singleton as union member #2" in {
    the[IllegalStateException].
      thrownBy(emit(ListSet(unionMember2Singleton))) should have message (
        "Cannot emit static members for properties of singleton 'ScalaRuntimeFixturesFamilyMember2': foo (string)")

  }

  it should "emit union" in {
    emit(ListSet(union1)) should equal("""export namespace ScalaRuntimeFixturesFamily {
  type Union = IScalaRuntimeFixturesFamilyMember1 | ScalaRuntimeFixturesFamilyMember2 | ScalaRuntimeFixturesFamilyMember3;

  public static fromData(data: any): ScalaRuntimeFixturesFamily {
    switch (data._type) {
      case "IScalaRuntimeFixturesFamilyMember1": {
        return ScalaRuntimeFixturesFamilyMember1.fromData(data);
      }
      case "ScalaRuntimeFixturesFamilyMember2": {
        return ScalaRuntimeFixturesFamilyMember2.fromData(data);
      }
      case "ScalaRuntimeFixturesFamilyMember3": {
        return ScalaRuntimeFixturesFamilyMember3.fromData(data);
      }
    }
  }

  public static toData(instance: ScalaRuntimeFixturesFamily): any {
    if (instance instanceof IScalaRuntimeFixturesFamilyMember1) {
      const data = ScalaRuntimeFixturesFamilyMember1.toData(instance);
      data['_type'] = "IScalaRuntimeFixturesFamilyMember1";
      return data;
    } else if (instance instanceof ScalaRuntimeFixturesFamilyMember2) {
      const data = ScalaRuntimeFixturesFamilyMember2.toData(instance);
      data['_type'] = "ScalaRuntimeFixturesFamilyMember2";
      return data;
    } else if (instance instanceof ScalaRuntimeFixturesFamilyMember3) {
      const data = ScalaRuntimeFixturesFamilyMember3.toData(instance);
      data['_type'] = "ScalaRuntimeFixturesFamilyMember3";
      return data;
    }
  }
}

export interface IScalaRuntimeFixturesFamily {
  foo: string;
}
""")
  }

  // ---

  private lazy val defaultConfig = Configuration(emitClasses = true)

  def emit(
    decls: ListSet[Declaration],
    config: Configuration = defaultConfig): String = {
    val buf = new java.io.ByteArrayOutputStream()
    lazy val out = new java.io.PrintStream(buf)

    val emiter = new TypeScriptEmitter(config, _ => out)

    try {
      emiter.emit(decls)
      out.flush()
      buf.toString
    } finally {
      out.close()
    }
  }
}
