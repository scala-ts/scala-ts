package com.mpc.scalats.core

import com.mpc.scalats.core.ScalaModel._
import org.scalatest.{ FlatSpec, Matchers }

import scala.reflect.runtime.universe.runtimeMirror

import scala.collection.immutable.ListSet

/**
 * Created by Milosz on 06.12.2016.
 */
final class ScalaParserSpec extends FlatSpec with Matchers {
  val scalaParser = new ScalaParser(Logger(
    org.slf4j.LoggerFactory getLogger "ScalaParserSpec"))

  import ScalaParserResults._

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

  case object TestObject1

  object TestObject2

  sealed trait Family

  case class FamilyMember1(foo: String) extends Family

  case object FamilyMember2 extends Family

  object FamilyMember3 extends Family
}

object ScalaParserResults {
  val caseClass1 = CaseClass("TestClass1", List(
    CaseClassMember("name", StringRef)), List.empty)

  val caseClass2 = CaseClass("TestClass2", List(
    CaseClassMember("name", TypeParamRef("T"))), List("T"))

  val caseClass3 = CaseClass(
    "TestClass3",
    List(CaseClassMember("name", SeqRef(TypeParamRef("T")))),
    List("T")
  )

  val caseClass4 = CaseClass("TestClass4", List(
    CaseClassMember("name",
      CaseClassRef("TestClass3", List(TypeParamRef("T"))))), List("T"))

  val caseClass5 = CaseClass(
    "TestClass5",
    List(CaseClassMember("name", OptionRef(TypeParamRef("T")))),
    List("T")
  )

  val caseClass6 = CaseClass("TestClass6", List(
    CaseClassMember("age", CaseClassRef("TestClass3", List(
      CaseClassRef("TestClass2", List(
        CaseClassRef("TestClass1", List.empty)))))),
    CaseClassMember("name", OptionRef(
      CaseClassRef("TestClass5",List(SeqRef(OptionRef(
        CaseClassRef("TestClass4",List(StringRef))))))))), List("T"))

  val caseClass7 = CaseClass(
    "TestClass7",
    List(CaseClassMember("name", UnionRef(ListSet(
      CaseClassRef("TestClass1", List.empty),
      CaseClassRef("TestClass1B", List.empty))))),
    List("T")
  )

  val caseObject1 = CaseObject("TestObject1")

  val caseObject2 = CaseObject("TestObject2")

  val sealedFamily1 = SealedUnion("Family", ListSet(
    CaseClass("FamilyMember1", List(
      CaseClassMember("foo", StringRef)), List.empty),
    CaseObject("FamilyMember2"),
    CaseObject("FamilyMember3")))
}
