package com.mpc.scalats.core

import scala.collection.immutable.ListSet

import org.scalatest.{ FlatSpec, Matchers }

import ScalaModel._

import scala.reflect.runtime.universe.runtimeMirror

/**
 * Created by Milosz on 06.12.2016.
 */
final class ScalaParserSpec extends FlatSpec with Matchers {
  import ScalaParserResults._

  val scalaParser = new ScalaParser(Logger(
    org.slf4j.LoggerFactory getLogger "ScalaParserSpec"))

  it should "parse case class with one primitive member" in {
    val parsed = scalaParser.parseTypes(List(ScalaFixtures.TestClass1Type))
    parsed should contain(caseClass1)
  }

  it should "parse generic case class with one member" in {
    val parsed = scalaParser.parseTypes(List(ScalaFixtures.TestClass2Type))
    parsed should contain(caseClass2)
  }

  it should "parse generic case class with one member list of type parameter" in {
    val parsed = scalaParser.parseTypes(List(ScalaFixtures.TestClass3Type))
    parsed should contain(caseClass3)
  }

  it should "parse generic case class with one optional member" in {
    val parsed = scalaParser.parseTypes(List(ScalaFixtures.TestClass5Type))
    parsed should contain(caseClass5)
  }

  it should "correctly detect involved types" in {
    val parsed = scalaParser.parseTypes(List(ScalaFixtures.TestClass6Type))

    parsed.size should equal(6)
    parsed should contain(caseClass1)
    parsed should contain(caseClass2)
    parsed should contain(caseClass3)
    parsed should contain(caseClass4)
    parsed should contain(caseClass5)
    parsed should contain(caseClass6)
  }

  it should "correctly handle either types" in {
    val parsed = scalaParser.parseTypes(List(ScalaFixtures.TestClass7Type))
    parsed should contain(caseClass7)
  }

  it should "parse case class with AnyVal-extends case class" in {
    val parsed = scalaParser.parseTypes(List(ScalaFixtures.TestClass8Type))
    parsed should contain(caseClass8)
  }

  it should "correctly parse case object" in {
    val parsed = scalaParser.parseTypes(List(ScalaFixtures.TestObject1Type))

    parsed should contain(caseObject1)
  }

  it should "correctly parse object" in {
    val parsed = scalaParser.parseTypes(List(ScalaFixtures.TestObject2Type))

    parsed should contain(caseObject2)
  }

  it should "correctly parse sealed trait as union" in {
    val parsed = scalaParser.parseTypes(List(ScalaFixtures.FamilyType))

    parsed should contain(sealedFamily1)
  }
}

object ScalaFixtures {
  implicit val mirror = runtimeMirror(getClass.getClassLoader)
  import mirror.universe
  import universe.typeOf

  val TestClass1Type = typeOf[TestClass1]
  val TestClass2Type = typeOf[TestClass2[_]]
  val TestClass3Type = typeOf[TestClass3[_]]
  val TestClass5Type = typeOf[TestClass5[_]]
  val TestClass6Type = typeOf[TestClass6[_]]
  val TestClass7Type = typeOf[TestClass7[_]]
  val TestClass8Type = typeOf[TestClass8]
  val TestObject1Type = typeOf[TestObject1.type]
  val TestObject2Type = typeOf[TestObject2.type]

  val FamilyType = typeOf[Family]

  case class TestClass1(name: String)

  case class TestClass1B(foo: String)

  case class TestClass2[T](name: T)

  case class TestClass3[T](name: List[T])

  case class TestClass4[T](name: TestClass3[T])

  case class TestClass5[T](name: Option[T])

  case class TestClass6[T](name: Option[TestClass5[List[Option[TestClass4[String]]]]], age: TestClass3[TestClass2[TestClass1]])

  case class TestClass7[T](name: Either[TestClass1, TestClass1B])

  case class CaseClassAnyVal(value: String) extends AnyVal

  case class TestClass8(name: CaseClassAnyVal)

  case object TestObject1

  object TestObject2

  sealed trait Family {
    def foo: String
    val bar = "lorem"
    def ipsum = 0.1D
  }

  case class FamilyMember1(foo: String) extends Family {
    @StableValue(typescript = "1")
    val code = 1
  }

  case object FamilyMember2 extends Family {
    // Members are unsupported for object,
    // and so the TS singleton won't implements the common interface
    val foo = "bar"
  }

  object FamilyMember3 extends Family {
    def foo = "lorem"
  }
}

object ScalaParserResults {
  val caseClass1 = CaseClass(
    name = "TestClass1",
    fields = ListSet(TypeMember("name", StringRef)),
    values = ListSet.empty,
    typeArgs = ListSet.empty)

  val caseClass2 = CaseClass(
    name = "TestClass2",
    fields = ListSet(TypeMember("name", TypeParamRef("T"))), 
    values = ListSet.empty,
    typeArgs = ListSet("T"))

  val caseClass3 = CaseClass(
    name = "TestClass3",
    fields = ListSet(TypeMember("name", SeqRef(TypeParamRef("T")))),
    values = ListSet.empty,
    typeArgs = ListSet("T")
  )

  val caseClass4 = CaseClass(
    name = "TestClass4",
    fields = ListSet(
      TypeMember("name", CaseClassRef("TestClass3",
        ListSet(TypeParamRef("T"))))),
    values = ListSet.empty,
    typeArgs = ListSet("T"))

  val caseClass5 = CaseClass(
    name = "TestClass5",
    fields = ListSet(TypeMember("name", OptionRef(TypeParamRef("T")))),
    values = ListSet.empty,
    typeArgs = ListSet("T")
  )

  val caseClass6 = CaseClass(
    name = "TestClass6",
    fields = ListSet(
      TypeMember("age", CaseClassRef("TestClass3", ListSet(
        CaseClassRef("TestClass2", ListSet(
          CaseClassRef("TestClass1", ListSet.empty)))))),
      TypeMember("name", OptionRef(
        CaseClassRef("TestClass5", ListSet(SeqRef(OptionRef(
          CaseClassRef("TestClass4", ListSet(StringRef))))))))),
    values = ListSet.empty,
    typeArgs = ListSet("T"))

  val caseClass7 = CaseClass(
    name = "TestClass7",
    fields = ListSet(TypeMember("name", UnionRef(ListSet(
      CaseClassRef("TestClass1", ListSet.empty),
      CaseClassRef("TestClass1B", ListSet.empty))))),
    values = ListSet.empty,
    typeArgs = ListSet("T")
  )

  val caseClass8 = CaseClass(
    name = "TestClass8",
    fields = ListSet(TypeMember("name", StringRef)),
    values = ListSet.empty,
    typeArgs = ListSet.empty)

  val caseObject1 = CaseObject("TestObject1", ListSet.empty)

  val caseObject2 = CaseObject("TestObject2", ListSet.empty)

  val sealedFamily1 = {
    // Not 'bar', as not abstract
    val foo = TypeMember("foo", StringRef)
    val code = TypeMember("code", IntRef)

    SealedUnion(
      "Family",
      ListSet(foo),
      ListSet(
        CaseClass(
          name = "FamilyMember1",
          fields = ListSet(foo),
          values = ListSet(code),
          typeArgs = ListSet.empty),
        CaseObject("FamilyMember2", ListSet(foo)),
        CaseObject("FamilyMember3", ListSet(foo))))
  }
}
