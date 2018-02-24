package com.mpc.scalats.core

import com.mpc.scalats.core.ScalaModel._
import org.scalatest._

import scala.reflect.runtime.universe._

/**
  * Created by Milosz on 06.12.2016.
  */
class ScalaParserSpec extends FlatSpec with Matchers {

  it should "parse trait with one primitive member" in {
    val parsed = ScalaParser.parseCaseClasses(List(TestTypes.TestClass1Type))
    val expected = Entity("TestClass1", List(EntityMember("name", StringRef)), List.empty, List.empty, true)
    parsed should contain(expected)
  }

  it should "parse generic case class with one member" in {
    val parsed = ScalaParser.parseCaseClasses(List(TestTypes.TestClass2Type))
    val expected = Entity("TestClass2", List(EntityMember("name", TypeParamRef("T"))), List("T"), List.empty, false)
    parsed should contain(expected)
  }

  it should "parse generic case class with one member list of type parameter" in {
    val parsed = ScalaParser.parseCaseClasses(List(TestTypes.TestClass3Type))
    val expected = Entity(
      "TestClass3",
      List(EntityMember("name", SeqRef(TypeParamRef("T")))),
      List("T"),
      List.empty,
      false
    )
    parsed should contain(expected)
  }

  it should "parse generic case class with one optional member" in {
    val parsed = ScalaParser.parseCaseClasses(List(TestTypes.TestClass5Type))
    val expected = Entity(
      "TestClass5",
      List(EntityMember("name", OptionRef(TypeParamRef("T")))),
      List("T"),
      List.empty,
      false
    )
    parsed should contain(expected)
  }

  it should "correctly detect involved types" in {
    val parsed = ScalaParser.parseCaseClasses(List(TestTypes.TestClass6Type))
    parsed should have length 6
  }

  it should "correctly handle either types" in {
    val parsed = ScalaParser.parseCaseClasses(List(TestTypes.TestClass7Type))
    val expected = Entity(
      "TestClass7",
      List(EntityMember("name", UnionRef(CaseClassRef("TestClass1", List()),CaseClassRef("TestClass1B", List())))),
      List("T"),
      List.empty,
      false
    )
    parsed should contain(expected)
  }

  it should "include base types of traits" in {
    val parser = ScalaParser.parseCaseClasses(List(TestTypes.TestTrait1Type))
    parser should have length 3
  }

}

object TestTypes {

  implicit val mirror = runtimeMirror(getClass.getClassLoader)
  val TestClass1Type = typeFromName("com.mpc.scalats.core.TestTypes.TestClass1")
  val TestClass2Type = typeFromName("com.mpc.scalats.core.TestTypes.TestClass2")
  val TestClass3Type = typeFromName("com.mpc.scalats.core.TestTypes.TestClass3")
  val TestClass4Type = typeFromName("com.mpc.scalats.core.TestTypes.TestClass4")
  val TestClass5Type = typeFromName("com.mpc.scalats.core.TestTypes.TestClass5")
  val TestClass6Type = typeFromName("com.mpc.scalats.core.TestTypes.TestClass6")
  val TestClass7Type = typeFromName("com.mpc.scalats.core.TestTypes.TestClass7")
  val TestTrait1Type = typeFromName("com.mpc.scalats.core.TestTypes.TestTrait1")

  private def typeFromName(name: String) = mirror.staticClass(name).toType

  trait TestClass1 {
    val name: String
  }

  case class TestClass1B(foo: String)

  case class TestClass2[T](name: T)

  case class TestClass3[T](name: List[T])

  case class TestClass4[T](name: TestClass3[T])

  case class TestClass5[T](name: Option[T])


  case class TestClass6[T](name: Option[TestClass5[List[Option[TestClass4[String]]]]], age: TestClass3[TestClass2[TestClass1]])

  case class TestClass7[T](name: Either[TestClass1, TestClass1B])

  sealed trait TestTrait1 {
    def id: Int
  }

  case class TestClass8(id: Int, other: String) extends TestTrait1

  case class TestClass9(id: Int, name: String) extends TestTrait1
}
