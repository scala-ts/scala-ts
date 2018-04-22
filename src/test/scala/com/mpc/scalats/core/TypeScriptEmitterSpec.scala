package com.mpc.scalats.core

import org.scalatest.{ FlatSpec, Matchers }

import scala.collection.immutable.ListSet

final class TypeScriptEmitterSpec extends FlatSpec with Matchers {
  import TypeScriptModel._
  import CompilerResults._

  it should "emit TypeScript class for a class with one primitive member" in {
    emit(ListSet(clazz1)) should equal("""export class TestClass1 {
	public name: string;

	constructor(
		name: string
	) {
		this.name = name;
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
    emit(ListSet(clazz2)) should equal("""export class TestClass2<T> {
	public name: T;

	constructor(
		name: T
	) {
		this.name = name;
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
    emit(ListSet(clazz3)) should equal("""export class TestClass3<T> {
	public name: T[];

	constructor(
		name: T[]
	) {
		this.name = name;
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
    emit(ListSet(clazz5)) should equal("""export class TestClass5<T> {
	public name: (T | null);

	constructor(
		name: (T | null)
	) {
		this.name = name;
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
    emit(ListSet(clazz7)) should equal("""export class TestClass7<T> {
	public name: (TestClass1 | TestClass1B);

	constructor(
		name: (TestClass1 | TestClass1B)
	) {
		this.name = name;
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
}
""")
  }

  it should "emit TypeScript class for a singleton #2" in {
    emit(ListSet(singleton2)) should equal("""export class TestObject2 {
	private static instance: TestObject2;

	private constructor() {}

	public static getInstance() {
		if (!TestObject2.instance) {
			TestObject2.instance = new TestObject2();
		}

		return TestObject2.instance;
	}
}
""")
  }

  it should "emit TypeScript union" in {
    emit(ListSet(union1)) should equal("""export type IFamily = IFamilyMember1 | FamilyMember2 | FamilyMember3;
""")
  }

  // ---

  private lazy val emiter = new TypeScriptEmitter("\t")

  def emit(decls: ListSet[Declaration]): String = {
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
