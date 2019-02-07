package com.mpc.scalats.core

import com.mpc.scalats.core.ScalaModel._
import org.scalatest._

import scala.collection.mutable
import scala.reflect.runtime.universe._
import ScalaModel.CaseClass
import com.mpc.scalats.configuration.Config

/**
  * Created by Milosz on 06.12.2016.
  */
class ScalaParserSpec extends FlatSpec with Matchers with BeforeAndAfterEach {

  override protected def beforeEach() = ScalaParser.alreadyExamined = mutable.Set.empty

  implicit val config = Config()

  it should "parse case class with one primitive member" in {
    val parsed = ScalaParser.parseTypes(List(TestTypes1.TestClass1Type))
    val expected = CaseClass("TestClass1", List(CaseClassMember("name", StringRef)), List.empty, None)
    parsed should contain(expected)
  }

  it should "parse generic case class with one member" in {
    val parsed = ScalaParser.parseTypes(List(TestTypes1.TestClass2Type))
    val expected = CaseClass("TestClass2", List(CaseClassMember("name", TypeParamRef("T"))), List("T"))
    parsed should contain(expected)
  }

  it should "parse generic case class with one member list of type parameter" in {
    val parsed = ScalaParser.parseTypes(List(TestTypes1.TestClass3Type))
    val expected = CaseClass(
      "TestClass3",
      List(CaseClassMember("name", SeqRef(TypeParamRef("T")))),
      List("T")
    )
    parsed should contain(expected)
  }

  it should "parse generic case class with one optional member" in {
    val parsed = ScalaParser.parseTypes(List(TestTypes1.TestClass5Type))
    val expected = CaseClass(
      "TestClass5",
      List(CaseClassMember("name", OptionRef(TypeParamRef("T")))),
      List("T")
    )
    parsed should contain(expected)
  }

  it should "parse empty sealed trait" in {
    val parsed = ScalaParser.parseTypes(List(TestTypes1.TestTrait1))
    val expected = CaseClass(
      "Trait1",
      Nil,
      Nil
    )
    parsed should contain(expected)
  }

  it should "correctly detect involved types" in {
    val parsed = ScalaParser.parseTypes(List(TestTypes1.TestTrait1, TestTypes1.TestClass6Type))
    parsed should have length 7
  }

  it should "correctly detect involved types with cascading sealed traits" in {
    val parsed = ScalaParser.parseTypes(List(TestTypes2.TestTrait1))
    parsed should have length 4
    parsed should contain(CaseClass("Trait1", Nil, Nil, None))
    parsed should contain(CaseClass("TestClass1", List(CaseClassMember(name = "name", UnknownTypeRef("Trait2"))), List.empty, Some("Trait1")))
    parsed should contain(CaseClass("Trait2", Nil, Nil, None))
    parsed should contain(CaseClass("TestClass2", List(CaseClassMember(name = "name", StringRef)), List.empty, Some("Trait2")))
  }

  it should "correctly detect involved types with case objects" in {
    val parsed = ScalaParser.parseTypes(List(TestTypes3.TestTrait1))
    parsed should have length 3
    parsed should contain(CaseClass("Trait1", Nil, Nil, None))
    parsed should contain(CaseClass("Object1", Nil, Nil, Some("Trait1")))
    parsed should contain(CaseClass("Object2", Nil, Nil, Some("Trait1")))
  }

  it should "correctly detect involved types with sealed trait hierarchies with intermediaries" in {
    val parsed = ScalaParser.parseTypes(List(TestTypes4.TestTrait1))
    parsed should have length 2
    parsed should contain(CaseClass("Trait4", Nil, Nil, None))
    parsed should contain(CaseClass("Class2", List(CaseClassMember(name = "name", StringRef)), List.empty, Some("Trait4")))
  }

  it should "correctly stop exploration on leaf types" in {
    implicit val configWithLeaf = Config(leafTypes = Set("com.mpc.scalats.core.TestTypes5.TestClass2"))
    val parsed = ScalaParser.parseTypes(List(TestTypes5.testSeed))(configWithLeaf)
    parsed should have length 1
    parsed should contain(CaseClass("TestClass1", List(CaseClassMember(name = "name", CaseClassRef("TestClass2", Nil))), Nil, None))
  }
}

object TestTypes1 {

  implicit val mirror = runtimeMirror(getClass.getClassLoader)
  val TestClass1Type = typeFromName("com.mpc.scalats.core.TestTypes1.TestClass1")
  val TestClass2Type = typeFromName("com.mpc.scalats.core.TestTypes1.TestClass2")
  val TestClass3Type = typeFromName("com.mpc.scalats.core.TestTypes1.TestClass3")
  val TestClass4Type = typeFromName("com.mpc.scalats.core.TestTypes1.TestClass4")
  val TestClass5Type = typeFromName("com.mpc.scalats.core.TestTypes1.TestClass5")
  val TestClass6Type = typeFromName("com.mpc.scalats.core.TestTypes1.TestClass6")
  val TestTrait1 = typeFromName("com.mpc.scalats.core.TestTypes1.Trait1")

  private def typeFromName(name: String) = mirror.staticClass(name).toType

  sealed trait Trait1

  case class TestClass1(name: String) extends Trait1

  case class TestClass2[T](name: T)

  case class TestClass3[T](name: List[T])

  case class TestClass4[T](name: TestClass3[T])

  case class TestClass5[T](name: Option[T])

  case class TestClass6[T](name: Option[TestClass5[List[Option[TestClass4[String]]]]], age: TestClass3[TestClass2[TestClass1]])

}

object TestTypes2 {

  implicit val mirror = runtimeMirror(getClass.getClassLoader)
  val TestTrait1 = typeFromName("com.mpc.scalats.core.TestTypes2.Trait1")

  private def typeFromName(name: String) = mirror.staticClass(name).toType

  sealed trait Trait1

  case class TestClass1(name: Trait2) extends Trait1

  sealed trait Trait2

  sealed abstract class Class2 extends Trait2

  case class TestClass2(name: String) extends Class2

}

object TestTypes3 {

  implicit val mirror = runtimeMirror(getClass.getClassLoader)

  val TestTrait1 = typeFromName("com.mpc.scalats.core.TestTypes3.Trait1")

  private def typeFromName(name: String) = mirror.staticClass(name).toType

  sealed trait Trait1

  case object Object1 extends Trait1

  case object Object2 extends Trait1

}

object TestTypes4 {

  implicit val mirror = runtimeMirror(getClass.getClassLoader)

  val TestTrait1 = typeFromName("com.mpc.scalats.core.TestTypes4.Trait4")

  private def typeFromName(name: String) = mirror.staticClass(name).toType

  sealed trait Trait4 {
    val getBidStrategyType: Option[String]
    val getBidStrategyValue: Option[Double]
    val getBidStrategyUnit: Option[String]
    val getBidStrategyDoNotExceed: Option[Double]
  }

  sealed abstract class Class1(bidStrategyType: Option[String],
    bidStrategyValue: Option[Double],
    bidStrategyUnit: Option[String],
    bidStrategyDoNotExceed: Option[Double]) extends Trait4 {
    override val getBidStrategyType: Option[String] = None
    override val getBidStrategyValue: Option[Double] = None
    override val getBidStrategyUnit: Option[String] = None
    override val getBidStrategyDoNotExceed: Option[Double] = None
  }

  case class Class2(name: String) extends Class1(None, None, Some(name), None)

}

object TestTypes5 {

  implicit val mirror = runtimeMirror(getClass.getClassLoader)
  val testSeed = typeFromName("com.mpc.scalats.core.TestTypes5.TestClass1")

  private def typeFromName(name: String) = mirror.staticClass(name).toType

  case class TestClass1(name: TestClass2)

  case class TestClass2(name: TestClass3)

  case class TestClass3(name: String)


}



