package com.mpc.scalats.core

import com.mpc.scalats.core.ScalaModel._
import org.scalatest._

import scala.reflect.runtime.universe._

/**
  * Created by Milosz on 06.12.2016.
  */
class ScalaParserSpec extends FlatSpec with Matchers {

  it should "parse case class with one primitive member" in {
    val parsed = ScalaParser.parseCaseClasses(List(TestTypes.TestClass1Type))
    val expected = CaseClass("TestClass1", List(CaseClassMember("name", StringRef)), List.empty)
    parsed should contain(expected)
  }

  it should "parse generic case class with one member" in {
    val parsed = ScalaParser.parseCaseClasses(List(TestTypes.TestClass2Type))
    val expected = CaseClass("TestClass2", List(CaseClassMember("name", TypeParamRef("T"))), List("T"))
    parsed should contain(expected)
  }

  it should "parse generic case class with one member list of type parameter" in {
    val parsed = ScalaParser.parseCaseClasses(List(TestTypes.TestClass3Type))
    val expected = CaseClass(
      "TestClass3",
      List(CaseClassMember("name", SeqRef(TypeParamRef("T")))),
      List("T")
    )
    parsed should contain(expected)
  }

  it should "parse generic case class with one optional member" in {
    val parsed = ScalaParser.parseCaseClasses(List(TestTypes.TestClass5Type))
    val expected = CaseClass(
      "TestClass5",
      List(CaseClassMember("name", OptionRef(TypeParamRef("T")))),
      List("T")
    )
    parsed should contain(expected)
  }

  it should "correctly detect involved types" in {
    val parsed = ScalaParser.parseCaseClasses(List(TestTypes.TestClass6Type))
    parsed.size should equal(6)
  }

  it should "correctly handle either types" in {
    val parsed = ScalaParser.parseCaseClasses(List(TestTypes.TestClass7Type))
    val expected = CaseClass(
      "TestClass7",
      List(CaseClassMember("name", UnionRef(CaseClassRef("TestClass1", List()),CaseClassRef("TestClass1B", List())))),
      List("T")
    )
    parsed should contain(expected)
  }

  it should "correctly parse case object" in {
    val parsed = ScalaParser.parseCaseClasses(List(TestTypes.TestObject1Type))
    val expected = CaseObject("TestObject1")

    parsed should contain(expected)
  }

  it should "correctly parse object" in {
    val parsed = ScalaParser.parseCaseClasses(List(TestTypes.TestObject2Type))
    val expected = CaseObject("TestObject2")

    parsed should contain(expected)
  }
}

object TestTypes {

  implicit val mirror = runtimeMirror(getClass.getClassLoader)
  import mirror.universe
  import universe.typeOf

  val TestClass1Type = typeOf[TestClass1]
  val TestClass2Type = typeOf[TestClass2[_]]
  val TestClass3Type = typeOf[TestClass3[_]]
  val TestClass4Type = typeOf[TestClass4[_]]
  val TestClass5Type = typeOf[TestClass5[_]]
  val TestClass6Type = typeOf[TestClass6[_]]
  val TestClass7Type = typeOf[TestClass7[_]]
  val TestObject1Type = typeOf[TestObject1.type]
  val TestObject2Type = typeOf[TestObject2.type]

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
}
