package com.mpc.scalats.core

import scala.collection.immutable.ListSet

import com.mpc.scalats.configuration.Config


import TypeScriptModel._
import ScalaParserResults._
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

final class CompilerSpec extends AnyFlatSpec with Matchers {
  import CompilerResults._

  implicit val defaultConfig: Config = Config(emitClasses = true)

  it should "compile a case class with one primitive member" in {
    val result = Compiler.compile(ListSet(caseClass1))

    result.size should equal(2)
    result should contain(interface1)
    result should contain(clazz1)
  }

  it should "compile a generic class with one member" in {
    val result = Compiler.compile(ListSet(caseClass2))

    result.size should equal(2)
    result should contain(interface2)
    result should contain(clazz2)
  }

  it should "compile a generic case class with one member list of type parameter" in {
    val result = Compiler.compile(ListSet(caseClass3))

    result.size should equal(2)
    result should contain(interface3)
    result should contain(clazz3)
  }

  it should "compile a generic case class with one optional member" in {
    val result = Compiler.compile(ListSet(caseClass5))

    result.size should equal(2)
    result should contain(interface5)
    result should contain(clazz5)
  }

  it should "compile disjunction types" in {
    val result = Compiler.compile(ListSet(caseClass7))

    result.size should equal(2)
    result should contain(interface7)
    result should contain(clazz7)
  }

  it should "compile parse case object" in {
    val result = Compiler.compile(ListSet(caseObject1))

    result.size should equal(1)
    result should contain(singleton1)
  }

  it should "correctly parse object" in {
    val result = Compiler.compile(
      ListSet(caseObject2),
      Some(InterfaceDeclaration(
        "SupI", ListSet.empty, ListSet.empty[String], Option.empty)))

    result.size should equal(1)
    result should contain(singleton2)
  }

  it should "correctly parse sealed trait as union" in {
    val result = Compiler.compile(ListSet(sealedFamily1))

    result.size should equal(5)
    result should contain(union1)

    val member1Interface = InterfaceDeclaration("IFamilyMember1",
      ListSet(Member("foo", StringRef)),
      ListSet.empty, Some(unionIface))

    result should contain(unionMember1Clazz)
    result should contain(member1Interface)
    result should contain(unionMember2Singleton)

    result should contain(
      SingletonDeclaration("FamilyMember3",
        ListSet(Member("foo", StringRef)), Some(unionIface)))
  }
}

object CompilerResults {
  val interface1 = InterfaceDeclaration("ITestClass1",
    ListSet(Member("name",StringRef)), ListSet.empty, Option.empty)

  val clazz1 = ClassDeclaration("TestClass1",
    ClassConstructor(ListSet(ClassConstructorParameter("name", StringRef))),
    ListSet.empty,
    ListSet.empty,
    Option.empty)

  val interface2 = InterfaceDeclaration("ITestClass2",
    ListSet(Member("name",SimpleTypeRef("T"))), ListSet("T"), Option.empty)

  val clazz2 = ClassDeclaration("TestClass2", ClassConstructor(ListSet(
    ClassConstructorParameter(
      "name", SimpleTypeRef("T")))), ListSet.empty, ListSet("T"), Option.empty)

  val interface3 = InterfaceDeclaration("ITestClass3",
    ListSet(Member("name", ArrayRef(SimpleTypeRef("T")))),
    ListSet("T"), Option.empty)

  val clazz3 = ClassDeclaration("TestClass3", ClassConstructor(ListSet(
    ClassConstructorParameter("name", ArrayRef(SimpleTypeRef("T"))))),
    ListSet.empty, ListSet("T"), Option.empty)

  val interface5 = InterfaceDeclaration("ITestClass5", ListSet(
    Member("name", UnionType(ListSet(SimpleTypeRef("T"), NullRef)))),
    ListSet("T"), Option.empty)

  val clazz5 = ClassDeclaration("TestClass5", ClassConstructor(ListSet(
    ClassConstructorParameter(
      "name", UnionType(ListSet(SimpleTypeRef("T"), NullRef))))),
    ListSet.empty, ListSet("T"), Option.empty)

  val interface7 = InterfaceDeclaration("ITestClass7", ListSet(
    Member("name", UnionType(ListSet(
      CustomTypeRef("ITestClass1", ListSet.empty),
      CustomTypeRef("ITestClass1B", ListSet.empty))))),
    ListSet("T"), Option.empty)

  val clazz7 = ClassDeclaration("TestClass7", ClassConstructor(ListSet(
    ClassConstructorParameter("name", UnionType(ListSet(
      CustomTypeRef("TestClass1", ListSet.empty),
      CustomTypeRef("TestClass1B", ListSet.empty)))))),
    ListSet.empty, ListSet("T"), Option.empty)

  val singleton1 = SingletonDeclaration(
    "TestObject1", ListSet.empty, Option.empty)

  val singleton2 = SingletonDeclaration("TestObject2", ListSet.empty, Some(
    InterfaceDeclaration("SupI", ListSet.empty, ListSet.empty[String], None)))

  val union1 = UnionDeclaration(
    name = "Family",
    fields = ListSet(Member("foo", StringRef)),
    possibilities = ListSet(
      CustomTypeRef("IFamilyMember1", ListSet.empty),
      CustomTypeRef("FamilyMember2", ListSet.empty),
      CustomTypeRef("FamilyMember3", ListSet.empty)),
    superInterface = Option.empty)

  val unionIface = InterfaceDeclaration(
    s"I${sealedFamily1.name}",
    ListSet(Member("foo", StringRef)),
    ListSet.empty[String],
    Option.empty)

  val unionMember1Clazz = ClassDeclaration(
    "FamilyMember1",
    constructor = ClassConstructor(ListSet(
      ClassConstructorParameter(
        "foo", StringRef))),
    values = ListSet(Member("code", NumberRef)),
    typeParams = ListSet.empty,
    superInterface = Some(unionIface))

  val unionMember2Singleton = SingletonDeclaration(
    "FamilyMember2", ListSet(Member("foo", StringRef)), Some(unionIface))

}
