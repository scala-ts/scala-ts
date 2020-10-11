package org.scalats.core

import scala.collection.immutable.ListSet

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

import TypeScriptModel._
import ScalaParserResults._

final class TranspilerSpec extends AnyFlatSpec with Matchers {
  import TranspilerResults._

  val defaultTranspiler: Transpiler =
    new Transpiler(Configuration(emitClasses = true))

  it should "compile a case class with one primitive member" in {
    val result = defaultTranspiler(ListSet(caseClass1))

    result.size should equal(2)
    result should contain(interface1)
    result should contain(clazz1)
  }

  it should "compile a generic class with one member" in {
    val result = defaultTranspiler(ListSet(caseClass2))

    result.size should equal(2)
    result should contain(interface2)
    result should contain(clazz2)
  }

  it should "compile a generic case class with one member list of type parameter" in {
    val result = defaultTranspiler(ListSet(caseClass3))

    result.size should equal(2)
    result should contain(interface3)
    result should contain(clazz3)
  }

  it should "compile a generic case class with one optional member" in {
    val result = defaultTranspiler(ListSet(caseClass5))

    result.size should equal(2)
    result should contain(interface5)
    result should contain(clazz5)
  }

  it should "compile disjunction types" in {
    val result = defaultTranspiler(ListSet(caseClass7))

    result.size should equal(2)
    result should contain(interface7)
    result should contain(clazz7)
  }

  it should "compile parse case object" in {
    val result = defaultTranspiler(ListSet(caseObject1))

    result.size should equal(1)
    result should contain(singleton1)
  }

  it should "correctly parse object" in {
    val result = defaultTranspiler(
      ListSet(caseObject2),
      Some(InterfaceDeclaration(
        "SupI", ListSet.empty, ListSet.empty[String], Option.empty)))

    result.size should equal(1)
    result should contain(singleton2)
  }

  it should "correctly parse sealed trait as union" in {
    val result = defaultTranspiler(ListSet(sealedFamily1))

    result.size should equal(5)
    result should contain(union1)

    val member1Interface = InterfaceDeclaration(
      "IScalaRuntimeFixturesFamilyMember1",
      ListSet(Member("foo", StringRef)),
      ListSet.empty, Some(unionIface))

    result should contain(unionMember1Clazz)
    result should contain(member1Interface)
    result should contain(unionMember2Singleton)

    result should contain(
      SingletonDeclaration(
        "ScalaRuntimeFixturesFamilyMember3",
        ListSet(Member("foo", StringRef)), Some(unionIface)))
  }
}

object TranspilerResults {
  val interface1 = InterfaceDeclaration(
    "IScalaRuntimeFixturesTestClass1",
    ListSet(Member("name", StringRef)), ListSet.empty, Option.empty)

  val clazz1 = ClassDeclaration(
    "ScalaRuntimeFixturesTestClass1",
    ClassConstructor(ListSet(ClassConstructorParameter("name", StringRef))),
    ListSet.empty,
    ListSet.empty,
    Option.empty)

  val interface2 = InterfaceDeclaration(
    "IScalaRuntimeFixturesTestClass2",
    ListSet(Member("name", SimpleTypeRef("T"))), ListSet("T"), Option.empty)

  val clazz2 = ClassDeclaration(
    "ScalaRuntimeFixturesTestClass2",
    ClassConstructor(ListSet(
      ClassConstructorParameter(
        "name", SimpleTypeRef("T")))), ListSet.empty, ListSet("T"), Option.empty)

  val interface3 = InterfaceDeclaration(
    "IScalaRuntimeFixturesTestClass3",
    ListSet(Member("name", ArrayRef(SimpleTypeRef("T")))),
    ListSet("T"), Option.empty)

  val clazz3 = ClassDeclaration(
    "ScalaRuntimeFixturesTestClass3", ClassConstructor(ListSet(
      ClassConstructorParameter("name", ArrayRef(SimpleTypeRef("T"))))),
    ListSet.empty, ListSet("T"), Option.empty)

  val interface5 = InterfaceDeclaration(
    "IScalaRuntimeFixturesTestClass5", ListSet(
      Member("name", UnionType(ListSet(SimpleTypeRef("T"), NullRef)))),
    ListSet("T"), Option.empty)

  val clazz5 = ClassDeclaration(
    "ScalaRuntimeFixturesTestClass5", ClassConstructor(ListSet(
      ClassConstructorParameter(
        "name", UnionType(ListSet(SimpleTypeRef("T"), NullRef))))),
    ListSet.empty, ListSet("T"), Option.empty)

  val interface7 = InterfaceDeclaration(
    "IScalaRuntimeFixturesTestClass7", ListSet(
      Member("name", UnionType(ListSet(
        CustomTypeRef("IScalaRuntimeFixturesTestClass1", ListSet.empty),
        CustomTypeRef("IScalaRuntimeFixturesTestClass1B", ListSet.empty))))),
    ListSet("T"), Option.empty)

  val clazz7 = ClassDeclaration(
    "ScalaRuntimeFixturesTestClass7", ClassConstructor(ListSet(
      ClassConstructorParameter("name", UnionType(ListSet(
        CustomTypeRef("ScalaRuntimeFixturesTestClass1", ListSet.empty),
        CustomTypeRef("ScalaRuntimeFixturesTestClass1B", ListSet.empty)))))),
    ListSet.empty, ListSet("T"), Option.empty)

  val singleton1 = SingletonDeclaration(
    "ScalaRuntimeFixturesTestObject1", ListSet.empty, Option.empty)

  val singleton2 = SingletonDeclaration(
    "ScalaRuntimeFixturesTestObject2", ListSet.empty, Some(
      InterfaceDeclaration("SupI", ListSet.empty, ListSet.empty[String], None)))

  val union1 = UnionDeclaration(
    name = "ScalaRuntimeFixturesFamily",
    fields = ListSet(Member("foo", StringRef)),
    possibilities = ListSet(
      CustomTypeRef("IScalaRuntimeFixturesFamilyMember1", ListSet.empty),
      CustomTypeRef("ScalaRuntimeFixturesFamilyMember2", ListSet.empty),
      CustomTypeRef("ScalaRuntimeFixturesFamilyMember3", ListSet.empty)),
    superInterface = Option.empty)

  val unionIface = InterfaceDeclaration(
    s"IScalaRuntimeFixtures${sealedFamily1.identifier.name}",
    ListSet(Member("foo", StringRef)),
    ListSet.empty[String],
    Option.empty)

  val unionMember1Clazz = ClassDeclaration(
    "ScalaRuntimeFixturesFamilyMember1",
    constructor = ClassConstructor(ListSet(
      ClassConstructorParameter(
        "foo", StringRef))),
    values = ListSet(Member("code", NumberRef)),
    typeParams = ListSet.empty,
    superInterface = Some(unionIface))

  val unionMember2Singleton = SingletonDeclaration(
    "ScalaRuntimeFixturesFamilyMember2",
    ListSet(Member("foo", StringRef)), Some(unionIface))

}
