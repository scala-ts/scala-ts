package io.github.scalats.core

import scala.collection.immutable.ListSet

import scala.reflect.runtime.{ universe => runtimeUniverse }

import ScalaModel._

final class ScalaParserSpec extends org.specs2.mutable.Specification {
  "Scala parser" title
  import ScalaParserResults._

  private implicit def cl: ClassLoader = getClass.getClassLoader

  val scalaParser = new ScalaParser[runtimeUniverse.type](
    universe = runtimeUniverse,
    logger = Logger(org.slf4j.LoggerFactory getLogger "ScalaParserSpec"))

  "Parser" should {
    "handle case class with one primitive member" in {
      val res = scalaParser.parseTypes(List(
        ScalaRuntimeFixtures.TestClass1Type))

      res.parsed must contain(caseClass1) and {
        res.parsed must have size 1
      }
    }

    "handle generic case class with one member" in {
      val res = scalaParser.parseTypes(List(
        ScalaRuntimeFixtures.TestClass2Type))

      res.parsed must contain(caseClass2) and {
        res.parsed must have size 1
      }
    }

    "handle generic case class with one member list of type parameter" in {
      val res = scalaParser.parseTypes(List(ScalaRuntimeFixtures.TestClass3Type))
      res.parsed must contain(caseClass3) and {
        res.parsed must have size 1
      }
    }

    "handle generic case class with one optional member" in {
      val res = scalaParser.parseTypes(List(ScalaRuntimeFixtures.TestClass5Type))

      res.parsed must contain(caseClass5) and {
        res.parsed must have size 1
      }
    }

    "detect involved types and skipped already examined types" in {
      val res = scalaParser.parseTypes(List(
        ScalaRuntimeFixtures.TestClass6Type,
        ScalaRuntimeFixtures.TestClass4Type, // skipped as examined from 6
        ScalaRuntimeFixtures.TestClass2Type, // skipped as examined from 6
        ScalaRuntimeFixtures.TestClass1Type // skipped as examined from 6
      ))

      import res.parsed

      parsed must contain(caseClass1) and {
        parsed must contain(caseClass2)
      } and {
        parsed must contain(caseClass3)
      } and {
        parsed must contain(caseClass4)
      } and {
        parsed must contain(caseClass5)
      } and {
        parsed must contain(caseClass6)
      } and {
        parsed must have size 6
      }
    }

    "handle either types" in {
      val res = scalaParser.parseTypes(
        List(ScalaRuntimeFixtures.TestClass7Type))

      import res.parsed

      parsed must contain(caseClass7) and {
        parsed must contain(caseClass1)
      } and {
        parsed must contain(caseClass1B)
      } and {
        parsed must have size 3
      }
    }

    "skip declaration ValueClass (as replaced by primitive)" in {
      val res = scalaParser.parseTypes(
        List(ScalaRuntimeFixtures.AnyValChildType))

      res.parsed must beEmpty
    }

    "handle ValueClass member as a primitive type" in {
      val res = scalaParser.parseTypes(List(ScalaRuntimeFixtures.TestClass8Type))

      res.parsed must contain(caseClass8) and {
        res.parsed must have size 1
      }
    }

    "handle enumeration" >> {
      "type declaration" in {
        val res = scalaParser.parseTypes(
          List(ScalaRuntimeFixtures.TestEnumerationType))

        res.parsed must contain(testEnumeration) and {
          res.parsed must have size 1
        }
      }

      "as member in class" in {
        val res = scalaParser.parseTypes(
          List(ScalaRuntimeFixtures.TestClass9Type))

        res.parsed must contain(caseClass9) and {
          res.parsed must contain(testEnumeration)
        } and {
          res.parsed must have size 2
        }
      }
    }

    "handle tuple values" in {
      val res = scalaParser.parseTypes(
        List(ScalaRuntimeFixtures.TestClass10Type))

      res.parsed must contain(caseClass10) and {
        res.parsed must have size 1
      }
    }

    "handle case object" in {
      val res = scalaParser.parseTypes(
        List(ScalaRuntimeFixtures.TestObject1Type))

      res.parsed must contain(caseObject1) and {
        res.parsed must have size 1
      }
    }

    "skip companion object" in {
      val res = scalaParser.parseTypes(List(
        ScalaRuntimeFixtures.TestClass1CompanionType))

      res.parsed must beEmpty
    }

    "handle object" in {
      val res = scalaParser.parseTypes(
        List(ScalaRuntimeFixtures.TestObject2Type))

      res.parsed must contain(caseObject2) and {
        res.parsed must have size 1
      }
    }

    "handle sealed trait as union" in {
      val res = scalaParser.parseTypes(
        List(ScalaRuntimeFixtures.FamilyType))

      res.parsed must contain(sealedFamily1) and {
        res.parsed must have size 1
      }
    }
  }
}

object ScalaRuntimeFixtures {
  implicit val mirror = runtimeUniverse.runtimeMirror(getClass.getClassLoader)
  import runtimeUniverse.typeOf

  val TestClass1Type = typeOf[TestClass1]
  val TestClass1CompanionType = typeOf[TestClass1.type]

  val TestClass2Type = typeOf[TestClass2[_]]
  val TestClass3Type = typeOf[TestClass3[_]]
  val TestClass4Type = typeOf[TestClass4[_]]
  val TestClass5Type = typeOf[TestClass5[_]]
  val TestClass6Type = typeOf[TestClass6[_]]
  val TestClass7Type = typeOf[TestClass7[_]]

  val AnyValChildType = typeOf[AnyValChild]
  val TestClass8Type = typeOf[TestClass8]

  val TestClass9Type = typeOf[TestClass9]
  val TestEnumerationType = typeOf[TestEnumeration.type]

  val TestClass10Type = typeOf[TestClass10]
  val TestObject1Type = typeOf[TestObject1.type]
  val TestObject2Type = typeOf[TestObject2.type]

  val FamilyType = typeOf[Family]

  case class TestClass1(name: String)

  case class TestClass1B(foo: String)

  case class TestClass2[T](name: T)

  case class TestClass3[T](name: List[T])

  case class TestClass4[T](name: TestClass3[T])

  case class TestClass5[T](
    name: Option[T],
    counters: Map[String, java.math.BigInteger])

  case class TestClass6[T](
    name: Option[TestClass5[List[Option[TestClass4[String]]]]],
    age: TestClass3[TestClass2[TestClass1]])

  case class TestClass7[T](name: Either[TestClass1, TestClass1B])

  case class AnyValChild(value: String) extends AnyVal

  case class TestClass8(name: AnyValChild)

  object TestEnumeration extends scala.Enumeration {
    val A, B, C = Value
  }

  case class TestClass9(name: TestEnumeration.Value)

  case class TestClass10(
    name: String,
    tuple: Tuple1[Int],
    tupleA: (String, Int),
    tupleB: Tuple2[String, Long],
    tupleC: Tuple3[String, String, Long])

  case object TestObject1

  object TestObject2

  sealed trait Family {
    def foo: String
    val bar = "lorem"
    def ipsum = 0.1D
  }

  case class FamilyMember1(foo: String) extends Family {
    @StableValue(typescript = "1") // TODO: Do? Check? Check is Literal tree
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
    identifier = QualifiedIdentifier(
      "TestClass1", List("ScalaRuntimeFixtures")),
    fields = ListSet(TypeMember("name", StringRef)),
    values = ListSet.empty,
    typeArgs = List.empty)

  val caseClass1B = CaseClass(
    identifier = QualifiedIdentifier(
      "TestClass1B", List("ScalaRuntimeFixtures")),
    fields = ListSet(TypeMember("foo", StringRef)),
    values = ListSet.empty,
    typeArgs = List.empty)

  val caseClass2 = CaseClass(
    identifier = QualifiedIdentifier(
      "TestClass2", List("ScalaRuntimeFixtures")),
    fields = ListSet(TypeMember("name", TypeParamRef("T"))),
    values = ListSet.empty,
    typeArgs = List("T"))

  val caseClass3 = CaseClass(
    identifier = QualifiedIdentifier(
      "TestClass3", List("ScalaRuntimeFixtures")),
    fields = ListSet(TypeMember("name", SeqRef(TypeParamRef("T")))),
    values = ListSet.empty,
    typeArgs = List("T"))

  val caseClass4 = CaseClass(
    identifier = QualifiedIdentifier(
      "TestClass4", List("ScalaRuntimeFixtures")),
    fields = ListSet(
      TypeMember("name", CaseClassRef(
        QualifiedIdentifier("TestClass3", List("ScalaRuntimeFixtures")),
        typeArgs = List(TypeParamRef("T"))))),
    values = ListSet.empty,
    typeArgs = List("T"))

  val caseClass5 = CaseClass(
    identifier = QualifiedIdentifier(
      "TestClass5", List("ScalaRuntimeFixtures")),
    fields = ListSet(
      TypeMember("counters", MapRef(StringRef, BigIntegerRef)),
      TypeMember("name", OptionRef(TypeParamRef("T")))),
    values = ListSet.empty,
    typeArgs = List("T"))

  val caseClass6 = CaseClass(
    identifier = QualifiedIdentifier(
      "TestClass6", List("ScalaRuntimeFixtures")),
    fields = ListSet(
      TypeMember("age", CaseClassRef(
        QualifiedIdentifier(
          "TestClass3", List("ScalaRuntimeFixtures")),
        typeArgs = List(
          CaseClassRef(
            QualifiedIdentifier(
              "TestClass2", List("ScalaRuntimeFixtures")),
            typeArgs = List(
              CaseClassRef(
                QualifiedIdentifier(
                  "TestClass1", List("ScalaRuntimeFixtures")),
                typeArgs = List.empty)))))),
      TypeMember("name", OptionRef(
        CaseClassRef(
          QualifiedIdentifier(
            "TestClass5", List("ScalaRuntimeFixtures")),
          typeArgs = List(SeqRef(OptionRef(
            CaseClassRef(
              QualifiedIdentifier(
                "TestClass4", List("ScalaRuntimeFixtures")),
              typeArgs = List(StringRef))))))))),
    values = ListSet.empty,
    typeArgs = List("T"))

  val caseClass7 = CaseClass(
    identifier = QualifiedIdentifier("TestClass7", List(
      "ScalaRuntimeFixtures")),
    fields = ListSet(TypeMember("name", UnionRef(ListSet(
      CaseClassRef(
        QualifiedIdentifier(
          "TestClass1", List("ScalaRuntimeFixtures")),
        typeArgs = List.empty),
      CaseClassRef(
        QualifiedIdentifier(
          "TestClass1B", List("ScalaRuntimeFixtures")),
        typeArgs = List.empty))))),
    values = ListSet.empty,
    typeArgs = List("T"))

  val caseClass8 = CaseClass(
    identifier = QualifiedIdentifier(
      "TestClass8", List("ScalaRuntimeFixtures")),
    fields = ListSet(TypeMember("name", StringRef)),
    values = ListSet.empty,
    typeArgs = List.empty)

  val caseClass9 = CaseClass(
    identifier = QualifiedIdentifier(
      "TestClass9", List("ScalaRuntimeFixtures")),
    fields = ListSet(TypeMember("name", EnumerationRef(
      QualifiedIdentifier("TestEnumeration", List("ScalaRuntimeFixtures"))))),
    values = ListSet.empty,
    typeArgs = List.empty)

  val testEnumeration = EnumerationDef(
    QualifiedIdentifier(
      "TestEnumeration", List("ScalaRuntimeFixtures")),
    ListSet("A", "B", "C"))

  val caseClass10 = CaseClass(
    identifier = QualifiedIdentifier(
      "TestClass10", List("ScalaRuntimeFixtures")),
    fields = ListSet(
      TypeMember("tupleC", TupleRef(List(StringRef, StringRef, LongRef))),
      TypeMember("tupleB", TupleRef(List(StringRef, LongRef))),
      TypeMember("tupleA", TupleRef(List(StringRef, IntRef))),
      TypeMember("tuple", TupleRef(List(IntRef))),
      TypeMember("name", StringRef)),
    values = ListSet.empty,
    typeArgs = List.empty)

  val caseObject1 = CaseObject(QualifiedIdentifier(
    "TestObject1", List("ScalaRuntimeFixtures")), ListSet.empty)

  val caseObject2 = CaseObject(QualifiedIdentifier(
    "TestObject2", List("ScalaRuntimeFixtures")), ListSet.empty)

  val sealedFamily1 = {
    // Not 'bar', as not abstract
    val foo = TypeMember("foo", StringRef)
    val code = TypeMember("code", IntRef)

    SealedUnion(
      QualifiedIdentifier("Family", List("ScalaRuntimeFixtures")),
      ListSet(foo),
      ListSet(
        CaseClass(
          identifier = QualifiedIdentifier(
            "FamilyMember1", List("ScalaRuntimeFixtures")),
          fields = ListSet(foo),
          values = ListSet(code),
          typeArgs = List.empty),
        CaseObject(QualifiedIdentifier(
          "FamilyMember2", List("ScalaRuntimeFixtures")), ListSet(foo)),
        CaseObject(QualifiedIdentifier(
          "FamilyMember3", List("ScalaRuntimeFixtures")), ListSet(foo))))
  }
}
