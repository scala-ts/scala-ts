package com.mpc.scalats.core

import scala.collection.immutable.ListSet

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

import com.mpc.scalats.configuration.{ Config, FieldNaming }

final class TypeScriptEmitterSpec extends AnyFlatSpec with Matchers {
  import TypeScriptModel._
  import CompilerResults._

  it should "emit TypeScript class for a class with one primitive member" in {
    emit(ListSet(clazz1)) should equal("""export class TestClass1 implements ITestClass1 {
	constructor(
		public name: string
	) {
		this.name = name;
	}

	public static fromData(data: any): TestClass1 {
		return <TestClass1>(data);
	}

	public static toData(instance: TestClass1): any {
		return instance;
	}
}
""")
  }

  it should "emit TypeScript interface for a class with one primitive member" in {
    emit(ListSet(interface1)) should equal("""export interface ITestClass1 {
	name: string;
}
""")
  }

  it should "emit TypeScript class for a class with generic member" in {
    emit(ListSet(clazz2)) should equal("""export class TestClass2<T> implements ITestClass2<T> {
	constructor(
		public name: T
	) {
		this.name = name;
	}

	public static fromData<T>(data: any): TestClass2<T> {
		return <TestClass2<T>>(data);
	}

	public static toData<T>(instance: TestClass2<T>): any {
		return instance;
	}
}
""")
  }

  it should "emit TypeScript interface for a class with generic member" in {
    emit(ListSet(interface2)) should equal("""export interface ITestClass2<T> {
	name: T;
}
""")
  }

  it should "emit TypeScript class for a class with generic array" in {
    emit(ListSet(clazz3)) should equal("""export class TestClass3<T> implements ITestClass3<T> {
	constructor(
		public name: T[]
	) {
		this.name = name;
	}

	public static fromData<T>(data: any): TestClass3<T> {
		return <TestClass3<T>>(data);
	}

	public static toData<T>(instance: TestClass3<T>): any {
		return instance;
	}
}
""")
  }

  it should "emit TypeScript interface for a class with generic array" in {
    emit(ListSet(interface3)) should equal("""export interface ITestClass3<T> {
	name: T[];
}
""")
  }

  it should "emit TypeScript class for a generic case class with one optional member" in {
    emit(ListSet(clazz5)) should equal("""export class TestClass5<T> implements ITestClass5<T> {
	constructor(
		public name: (T | null)
	) {
		this.name = name;
	}

	public static fromData<T>(data: any): TestClass5<T> {
		return <TestClass5<T>>(data);
	}

	public static toData<T>(instance: TestClass5<T>): any {
		return instance;
	}
}
""")
  }

  it should "emit TypeScript interface for a generic case class with one optional member" in {
    emit(ListSet(interface5)) should equal("""export interface ITestClass5<T> {
	name: (T | null);
}
""")
  }

  it should "emit TypeScript class for a generic case class with disjunction" in {
    emit(ListSet(clazz7)) should equal("""export class TestClass7<T> implements ITestClass7<T> {
	constructor(
		public name: (TestClass1 | TestClass1B)
	) {
		this.name = name;
	}

	public static fromData<T>(data: any): TestClass7<T> {
		return <TestClass7<T>>(data);
	}

	public static toData<T>(instance: TestClass7<T>): any {
		return instance;
	}
}
""")
  }

  it should "emit TypeScript interface for a generic case class with disjunction" in {
    emit(ListSet(interface7)) should equal("""export interface ITestClass7<T> {
	name: (ITestClass1 | ITestClass1B);
}
""")
  }

  it should "emit TypeScript class using FieldNaming.SnakeCase" in {
    val clazz = ClassDeclaration("Test", ClassConstructor(ListSet(
      ClassConstructorParameter("name", SimpleTypeRef("T")),
      ClassConstructorParameter("fooBar", TypeScriptModel.StringRef))),
      ListSet.empty,
      ListSet("T"), Option.empty)

    val config = defaultConfig.copy(fieldNaming = FieldNaming.SnakeCase)

    emit(ListSet(clazz), config) should equal("""export class Test<T> implements ITest<T> {
	constructor(
		public name: T,
		public fooBar: string
	) {
		this.name = name;
		this.fooBar = fooBar;
	}

	public static fromData<T>(data: any): Test<T> {
		return new Test<T>(data.name, data.foo_bar);
	}

	public static toData<T>(instance: Test<T>): any {
		return {
			name: instance.name,
			foo_bar: instance.fooBar
		};
	}
}
""")
  }

  it should "emit TypeScript class for a singleton #1" in {
    emit(ListSet(singleton1)) should equal("""export class TestObject1 {
	private static instance: TestObject1;

	private constructor() {}

	public static getInstance() {
		if (!TestObject1.instance) {
			TestObject1.instance = new TestObject1();
		}

		return TestObject1.instance;
	}

	public static fromData(data: any): TestObject1 {
		return TestObject1.instance;
	}

	public static toData(instance: TestObject1): any {
		return instance;
	}
}
""")
  }

  it should "emit TypeScript class for a singleton #2" in {
    // SCALATS1: No implements SupI
    emit(ListSet(singleton2)) should equal("""export class TestObject2 implements SupI {
	private static instance: TestObject2;

	private constructor() {}

	public static getInstance() {
		if (!TestObject2.instance) {
			TestObject2.instance = new TestObject2();
		}

		return TestObject2.instance;
	}

	public static fromData(data: any): TestObject2 {
		return TestObject2.instance;
	}

	public static toData(instance: TestObject2): any {
		return instance;
	}
}
""")
  }

  it should "emit TypeScript class as union member #1" in {
    the[IllegalStateException].
      thrownBy(emit(ListSet(unionMember1Clazz))) should have message(
      "Cannot emit static members for class values: code (number)")

  }

  it should "emit TypeScript singleton as union member #2" in {
    the[IllegalStateException].
      thrownBy(emit(ListSet(unionMember2Singleton))) should have message(
      "Cannot emit static members for singleton values: foo (string)")

  }

  it should "emit TypeScript union" in {
    emit(ListSet(union1)) should equal("""export namespace Family {
	type Union = IFamilyMember1 | FamilyMember2 | FamilyMember3;

	public static fromData(data: any): Family {
		switch (data._type) {
			case "IFamilyMember1": {
				return FamilyMember1.fromData(data);
			}
			case "FamilyMember2": {
				return FamilyMember2.fromData(data);
			}
			case "FamilyMember3": {
				return FamilyMember3.fromData(data);
			}
		}
	}

	public static toData(instance: Family): any {
		if (instance instanceof IFamilyMember1) {
			const data = FamilyMember1.toData(instance);
			data['_type'] = "IFamilyMember1";
			return data;
		} else if (instance instanceof FamilyMember2) {
			const data = FamilyMember2.toData(instance);
			data['_type'] = "FamilyMember2";
			return data;
		} else if (instance instanceof FamilyMember3) {
			const data = FamilyMember3.toData(instance);
			data['_type'] = "FamilyMember3";
			return data;
		}
	}
}

export interface IFamily {
	foo: string;
}
""")
  }

  // ---

  private lazy val defaultConfig = Config(emitClasses = true)

  def emit(
    decls: ListSet[Declaration],
    config: Config = defaultConfig): String = {
    val emiter = new TypeScriptEmitter(config)
    val buf = new java.io.ByteArrayOutputStream()
    lazy val out = new java.io.PrintStream(buf)

    try {
      emiter.emit(decls, out)
      out.flush()
      buf.toString
    } finally {
      out.close()
    }
  }
}
