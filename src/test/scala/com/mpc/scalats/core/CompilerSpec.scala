package com.mpc.scalats.core

import com.mpc.scalats.configuration.Config
import com.mpc.scalats.core.TypeScriptModel._

import org.scalatest.{ FlatSpec, Matchers }

import scala.collection.immutable.ListSet

final class CompilerSpec extends FlatSpec with Matchers {
  import ScalaParserResults._
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
    val result = Compiler.compile(ListSet(caseObject2))

    result.size should equal(1)
    result should contain(singleton2)
  }

  it should "correctly parse sealed trait as union" in {
    val result = Compiler.compile(ListSet(sealedFamily1))

    result.size should equal(5)
    result should contain(union1)

    val member1Clazz = ClassDeclaration("FamilyMember1", ClassConstructor(List(
      ClassConstructorParameter(
        "foo", StringRef, Some(AccessModifier.Public)))), List.empty)

    val member1Interface = InterfaceDeclaration("IFamilyMember1",
      List(Member("foo", StringRef)), List.empty)

    result should contain(member1Clazz)
    result should contain(member1Interface)
    result should contain(SingletonDeclaration("FamilyMember2"))
    result should contain(SingletonDeclaration("FamilyMember3"))
  }
}

object CompilerResults {
  val interface1 = InterfaceDeclaration("ITestClass1",
    List(Member("name",StringRef)), List.empty)

  val clazz1 = ClassDeclaration("TestClass1", ClassConstructor(List(
    ClassConstructorParameter(
      "name", StringRef, Some(AccessModifier.Public)))), List.empty)

  val interface2 = InterfaceDeclaration("ITestClass2",
    List(Member("name",TypeParamRef("T"))),List("T"))

  val clazz2 = ClassDeclaration("TestClass2", ClassConstructor(List(
    ClassConstructorParameter(
      "name", TypeParamRef("T"), Some(AccessModifier.Public)))), List("T"))

  val interface3 = InterfaceDeclaration("ITestClass3",
    List(Member("name", ArrayRef(TypeParamRef("T")))),List("T"))

  val clazz3 = ClassDeclaration("TestClass3", ClassConstructor(List(
    ClassConstructorParameter(
      "name", ArrayRef(TypeParamRef("T")),
      Some(AccessModifier.Public)))), List("T"))

  val interface5 = InterfaceDeclaration("ITestClass5", List(
    Member("name", UnionType(ListSet(TypeParamRef("T"), NullRef)))), List("T"))

  val clazz5 = ClassDeclaration("TestClass5", ClassConstructor(List(
    ClassConstructorParameter(
      "name", UnionType(ListSet(TypeParamRef("T"), NullRef)),
      Some(AccessModifier.Public)))), List("T"))

  val interface7 = InterfaceDeclaration("ITestClass7", List(
    Member("name", UnionType(ListSet(CustomTypeRef("ITestClass1", List.empty),
      CustomTypeRef("ITestClass1B", List.empty))))), List("T"))

  val clazz7 = ClassDeclaration("TestClass7", ClassConstructor(List(
    ClassConstructorParameter("name", UnionType(ListSet(
      CustomTypeRef("TestClass1",List.empty),
      CustomTypeRef("TestClass1B", List.empty))),
      Some(AccessModifier.Public)))), List("T"))

  val singleton1 = SingletonDeclaration("TestObject1")

  val singleton2 = SingletonDeclaration("TestObject2")

  val union1 = UnionDeclaration("IFamily", ListSet(
    UnknownTypeRef("IFamilyMember1"),
    UnknownTypeRef("FamilyMember2"),
    UnknownTypeRef("FamilyMember3")))
}
