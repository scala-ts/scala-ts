package com.mpc.scalats.core

import org.scalatest._
import scala.reflect.runtime.universe._

import com.mpc.scalats.core.ScalaModel._
import com.mpc.scalats.core.TestTypes.TestClass3

/**
  * Created by Milosz on 06.12.2016.
  */
class ScalaParserSpec extends FlatSpec with Matchers {

  it should "parse case class with one primitive member" in {
    val parsed = ScalaParser.parseCaseClasses(List(TestTypes.TestClass1Type))
    val expected = CaseClass("TestClass1", List(CaseClassMember("name", StringRef)), List.empty)
    parsed("TestClass1") shouldEqual expected
  }

  it should "parse generic case class with one member" in {
    val parsed = ScalaParser.parseCaseClasses(List(TestTypes.TestClass2Type))
    val expected = CaseClass("TestClass2", List(CaseClassMember("name", TypeParamRef("T"))), List("T"))
    parsed("TestClass2") shouldEqual expected
  }

  it should "parse generic case class with one member list of type parameter" in {
    val parsed = ScalaParser.parseCaseClasses(List(TestTypes.TestClass3Type))
    val expected = CaseClass(
      "TestClass3",
      List(CaseClassMember("name", SeqRef(TypeParamRef("T")))),
      List("T")
    )
    parsed("TestClass3") shouldEqual expected
  }

  it should "parse generic case class with one optional member" in {
    val parsed = ScalaParser.parseCaseClasses(List(TestTypes.TestClass5Type))
    val expected = CaseClass(
      "TestClass5",
      List(CaseClassMember("name", OptionRef(TypeParamRef("T")))),
      List("T")
    )
    parsed("TestClass5") shouldEqual expected
  }

}

object TestTypes {

  implicit val mirror = runtimeMirror(getClass.getClassLoader)

  case class TestClass1(name: String)

  case class TestClass2[T](name: T)

  case class TestClass3[T](name: List[T])

  case class TestClass4[T](name: TestClass3[T])

  case class TestClass5[T](name: Option[T])

  val TestClass1Type = typeFromName("com.mpc.scalats.core.TestTypes.TestClass1")

  val TestClass2Type = typeFromName("com.mpc.scalats.core.TestTypes.TestClass2")

  val TestClass3Type = typeFromName("com.mpc.scalats.core.TestTypes.TestClass3")

  val TestClass4Type = typeFromName("com.mpc.scalats.core.TestTypes.TestClass4")

  val TestClass5Type = typeFromName("com.mpc.scalats.core.TestTypes.TestClass5")

  private def typeFromName(name: String) = mirror.staticClass(name).toType
}
