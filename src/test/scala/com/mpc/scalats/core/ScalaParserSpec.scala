package com.mpc.scalats.core

import scala.collection.immutable.ListSet

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import org.slf4j.LoggerFactory

import ScalaModel._

import scala.reflect.runtime.universe.runtimeMirror

final class ScalaParserSpec extends AnyFlatSpec with Matchers {
  import ScalaParserResults._

  val logger = Logger(LoggerFactory.getLogger(getClass.getSimpleName))
  val mirror = runtimeMirror(getClass.getClassLoader)

  val scalaParser = new ScalaParser(logger, mirror)

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

  it should "correctly parse case class extends AnyVal as a primitive type" in {
    val parsed = scalaParser.parseTypes(List(ScalaFixtures.TestClass8Type))
    parsed should contain(caseClass8)
  }

  it should "correctly handle enumeration values" in {
    val parsed = scalaParser.parseTypes(List(ScalaFixtures.TestClass9Type))
    parsed should contain(caseClass9)
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
  val TestClass9Type = typeOf[TestClass9]
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

  case class AnyValChild(value: String) extends AnyVal

  case class TestClass8(name: AnyValChild)

  object TestEnumeration extends scala.Enumeration {
    val A, B, C = Value
  }

  case class TestClass9(name: TestEnumeration.Value)

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
    identifier = QualifiedIdentifier("TestClass1", List("ScalaFixtures")),
    fields = ListSet(TypeMember("name", StringRef)),
    values = ListSet.empty,
    typeArgs = ListSet.empty)

  val caseClass2 = CaseClass(
    identifier = QualifiedIdentifier("TestClass2", List("ScalaFixtures")),
    fields = ListSet(TypeMember("name", TypeParamRef("T"))), 
    values = ListSet.empty,
    typeArgs = ListSet("T"))

  val caseClass3 = CaseClass(
    identifier = QualifiedIdentifier("TestClass3", List("ScalaFixtures")),
    fields = ListSet(TypeMember("name", SeqRef(TypeParamRef("T")))),
    values = ListSet.empty,
    typeArgs = ListSet("T")
  )

  val caseClass4 = CaseClass(
    identifier = QualifiedIdentifier("TestClass4", List("ScalaFixtures")),
    fields = ListSet(
      TypeMember("name", CaseClassRef(QualifiedIdentifier("TestClass3", List("ScalaFixtures")),
        ListSet(TypeParamRef("T"))))),
    values = ListSet.empty,
    typeArgs = ListSet("T"))

  val caseClass5 = CaseClass(
    identifier = QualifiedIdentifier("TestClass5", List("ScalaFixtures")),
    fields = ListSet(TypeMember("name", OptionRef(TypeParamRef("T")))),
    values = ListSet.empty,
    typeArgs = ListSet("T")
  )

  val caseClass6 = CaseClass(
    identifier = QualifiedIdentifier("TestClass6", List("ScalaFixtures")),
    fields = ListSet(
      TypeMember("age", CaseClassRef(QualifiedIdentifier("TestClass3", List("ScalaFixtures")), ListSet(
        CaseClassRef(QualifiedIdentifier("TestClass2", List("ScalaFixtures")), ListSet(
          CaseClassRef(QualifiedIdentifier("TestClass1", List("ScalaFixtures")), ListSet.empty)))))),
      TypeMember("name", OptionRef(
        CaseClassRef(QualifiedIdentifier("TestClass5", List("ScalaFixtures")), ListSet(SeqRef(OptionRef(
          CaseClassRef(QualifiedIdentifier("TestClass4", List("ScalaFixtures")), ListSet(StringRef))))))))),
    values = ListSet.empty,
    typeArgs = ListSet("T"))

  val caseClass7 = CaseClass(
    identifier = QualifiedIdentifier("TestClass7", List("ScalaFixtures")),
    fields = ListSet(TypeMember("name", UnionRef(ListSet(
      CaseClassRef(QualifiedIdentifier("TestClass1", List("ScalaFixtures")), ListSet.empty),
      CaseClassRef(QualifiedIdentifier("TestClass1B", List("ScalaFixtures")), ListSet.empty))))),
    values = ListSet.empty,
    typeArgs = ListSet("T")
  )

  val caseClass8 = CaseClass(
    identifier = QualifiedIdentifier("TestClass8", List("ScalaFixtures")),
    fields = ListSet(TypeMember("name", StringRef)),
    values = ListSet.empty,
    typeArgs = ListSet.empty)

  val caseClass9 = CaseClass(
    identifier = QualifiedIdentifier("TestClass9", List("ScalaFixtures")),
    fields = ListSet(TypeMember("name", EnumerationRef(QualifiedIdentifier("TestEnumeration", List("ScalaFixtures"))))),
    values = ListSet.empty,
    typeArgs = ListSet.empty)

  val caseObject1 = CaseObject(QualifiedIdentifier("TestObject1", List("ScalaFixtures")), ListSet.empty)

  val caseObject2 = CaseObject(QualifiedIdentifier("TestObject2", List("ScalaFixtures")), ListSet.empty)

  val sealedFamily1 = {
    // Not 'bar', as not abstract
    val foo = TypeMember("foo", StringRef)
    val code = TypeMember("code", IntRef)

    SealedUnion(
      QualifiedIdentifier("Family", List("ScalaFixtures")),
      ListSet(foo),
      ListSet(
        CaseClass(
          identifier = QualifiedIdentifier("FamilyMember1", List("ScalaFixtures")),
          fields = ListSet(foo),
          values = ListSet(code),
          typeArgs = ListSet.empty),
        CaseObject(QualifiedIdentifier("FamilyMember2", List("ScalaFixtures")), ListSet(foo)),
        CaseObject(QualifiedIdentifier("FamilyMember3", List("ScalaFixtures")), ListSet(foo))))
  }
}
