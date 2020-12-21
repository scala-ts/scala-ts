package io.github.scalats.core

import scala.collection.immutable.ListSet

import io.github.scalats.typescript._

import ScalaParserResults._

final class TranspilerSpec extends org.specs2.mutable.Specification {
  "Transpiler" title

  import TranspilerResults._

  val defaultTranspiler: Transpiler =
    new Transpiler(Configuration(emitClasses = true))

  "Transpiler" should {
    "transpile a case class with one primitive member" in {
      val result = defaultTranspiler(ListSet(caseClass1))

      result must have size 2 and {
        result must contain(interface1)
      } and {
        result must contain(clazz1)
      }
    }

    "transpile a generic class with one member" in {
      val result = defaultTranspiler(ListSet(caseClass2))

      result must have size 2 and {
        result must contain(interface2)
      } and {
        result must contain(clazz2)
      }
    }

    "transpile a generic case class with one member list of type parameter" in {
      val result = defaultTranspiler(ListSet(caseClass3))

      result must have size 2 and {
        result must contain(interface3)
      } and {
        result must contain(clazz3)
      }
    }

    "transpile a generic case class with one optional member" in {
      val result = defaultTranspiler(ListSet(caseClass5))

      result must have size 2 and {
        result must contain(interface5)
      } and {
        result must contain(clazz5)
      }
    }

    "transpile disjunction types" in {
      val result = defaultTranspiler(ListSet(caseClass7))

      result must have size 2 and {
        result must contain(interface7)
      } and {
        result must contain(clazz7)
      }
    }

    "transpile Tuple types" in {
      val result = defaultTranspiler(ListSet(caseClass10))

      result must have size 2 and {
        result must contain(interface10)
      } and {
        result must contain(clazz10)
      }
    }

    "transpile case object" in {
      val result = defaultTranspiler(ListSet(caseObject1))

      result must have size 1 and {
        result must contain(singleton1)
      }
    }

    "correctly transpile object" in {
      val result = defaultTranspiler(
        ListSet(caseObject2),
        Some(InterfaceDeclaration(
          "SupI", ListSet.empty, List.empty[String], Option.empty)))

      result must have size 1 and {
        result must contain(singleton2)
      }
    }

    "correctly transpile sealed trait as union" in {
      val result = defaultTranspiler(ListSet(sealedFamily1))

      result must have size 5 and {
        result must contain(union1)
      } and {
        result must contain(unionMember1Clazz)
      } and {
        result must contain(unionMember2Singleton)
      } and {

        val member1Interface = InterfaceDeclaration(
          "IScalaRuntimeFixturesFamilyMember1",
          ListSet(Member("foo", StringRef)),
          List.empty, Some(unionIface))

        result must contain(member1Interface)
      } and {
        result must contain(
          SingletonDeclaration(
            "ScalaRuntimeFixturesFamilyMember3",
            ListSet(Member("foo", StringRef)), Some(unionIface)))
      }
    }
  }
}

object TranspilerResults {
  val interface1 = InterfaceDeclaration(
    "IScalaRuntimeFixturesTestClass1",
    ListSet(Member("name", StringRef)), List.empty, Option.empty)

  val clazz1 = ClassDeclaration(
    "ScalaRuntimeFixturesTestClass1",
    ClassConstructor(ListSet(ClassConstructorParameter("name", StringRef))),
    ListSet.empty,
    typeParams = List.empty,
    superInterface = Option.empty)

  val interface2 = InterfaceDeclaration(
    "IScalaRuntimeFixturesTestClass2",
    ListSet(Member("name", SimpleTypeRef("T"))),
    typeParams = List("T"),
    superInterface = Option.empty)

  val clazz2 = ClassDeclaration(
    "ScalaRuntimeFixturesTestClass2",
    ClassConstructor(ListSet(
      ClassConstructorParameter("name", SimpleTypeRef("T")))),
    ListSet.empty,
    typeParams = List("T"),
    superInterface = Option.empty)

  val interface3 = InterfaceDeclaration(
    "IScalaRuntimeFixturesTestClass3",
    ListSet(Member("name", ArrayRef(SimpleTypeRef("T")))),
    typeParams = List("T"),
    superInterface = Option.empty)

  val clazz3 = ClassDeclaration(
    "ScalaRuntimeFixturesTestClass3", ClassConstructor(ListSet(
      ClassConstructorParameter("name", ArrayRef(SimpleTypeRef("T"))))),
    ListSet.empty,
    typeParams = List("T"),
    superInterface = Option.empty)

  val interface5 = InterfaceDeclaration(
    "IScalaRuntimeFixturesTestClass5", ListSet(
      Member("counters", MapType(StringRef, NumberRef)),
      Member("name", NullableType(SimpleTypeRef("T")))),
    typeParams = List("T"),
    superInterface = Option.empty)

  val clazz5 = ClassDeclaration(
    "ScalaRuntimeFixturesTestClass5", ClassConstructor(ListSet(
      ClassConstructorParameter(
        "counters", MapType(StringRef, NumberRef)),
      ClassConstructorParameter(
        "name", NullableType(SimpleTypeRef("T"))))),
    ListSet.empty,
    typeParams = List("T"),
    superInterface = Option.empty)

  val interface7 = InterfaceDeclaration(
    "IScalaRuntimeFixturesTestClass7", ListSet(
      Member("name", UnionType(ListSet(
        CustomTypeRef("IScalaRuntimeFixturesTestClass1", List.empty),
        CustomTypeRef("IScalaRuntimeFixturesTestClass1B", List.empty))))),
    typeParams = List("T"),
    superInterface = Option.empty)

  val clazz7 = ClassDeclaration(
    "ScalaRuntimeFixturesTestClass7", ClassConstructor(ListSet(
      ClassConstructorParameter("name", UnionType(ListSet(
        CustomTypeRef("ScalaRuntimeFixturesTestClass1", List.empty),
        CustomTypeRef("ScalaRuntimeFixturesTestClass1B", List.empty)))))),
    ListSet.empty,
    typeParams = List("T"),
    superInterface = Option.empty)

  val interface10 = InterfaceDeclaration(
    "IScalaRuntimeFixturesTestClass10", ListSet(
      Member("tupleC", TupleRef(List(StringRef, StringRef, NumberRef))),
      Member("tupleB", TupleRef(List(StringRef, NumberRef))),
      Member("tupleA", TupleRef(List(StringRef, NumberRef))),
      Member("tuple", TupleRef(List(NumberRef))),
      Member("name", StringRef)),
    typeParams = List.empty,
    superInterface = None)

  val clazz10 = ClassDeclaration(
    "ScalaRuntimeFixturesTestClass10", ClassConstructor(ListSet(
      ClassConstructorParameter(
        "tupleC", TupleRef(List(StringRef, StringRef, NumberRef))),
      ClassConstructorParameter(
        "tupleB", TupleRef(List(StringRef, NumberRef))),
      ClassConstructorParameter(
        "tupleA", TupleRef(List(StringRef, NumberRef))),
      ClassConstructorParameter("tuple", TupleRef(List(NumberRef))),
      ClassConstructorParameter("name", StringRef))),
    ListSet.empty,
    typeParams = List.empty,
    superInterface = None)

  val singleton1 = SingletonDeclaration(
    "ScalaRuntimeFixturesTestObject1", ListSet.empty, Option.empty)

  val singleton2 = SingletonDeclaration(
    "ScalaRuntimeFixturesTestObject2", ListSet.empty, Some(
      InterfaceDeclaration("SupI", ListSet.empty, List.empty[String], None)))

  val union1 = UnionDeclaration(
    name = "ScalaRuntimeFixturesFamily",
    fields = ListSet(Member("foo", StringRef)),
    possibilities = ListSet(
      CustomTypeRef("IScalaRuntimeFixturesFamilyMember1", List.empty),
      CustomTypeRef("ScalaRuntimeFixturesFamilyMember2", List.empty),
      CustomTypeRef("ScalaRuntimeFixturesFamilyMember3", List.empty)),
    superInterface = Option.empty)

  val unionIface = InterfaceDeclaration(
    s"IScalaRuntimeFixtures${sealedFamily1.identifier.name}",
    ListSet(Member("foo", StringRef)),
    typeParams = List.empty[String],
    superInterface = Option.empty)

  val unionMember1Clazz = ClassDeclaration(
    "ScalaRuntimeFixturesFamilyMember1",
    constructor = ClassConstructor(ListSet(
      ClassConstructorParameter(
        "foo", StringRef))),
    values = ListSet(Member("code", NumberRef)),
    typeParams = List.empty,
    superInterface = Some(unionIface))

  val unionMember2Singleton = SingletonDeclaration(
    "ScalaRuntimeFixturesFamilyMember2",
    ListSet(Member("foo", StringRef)), Some(unionIface))

}
