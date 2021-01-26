package io.github.scalats.core

import scala.util.control.NonFatal

import scala.collection.immutable.ListSet

import scala.reflect.runtime.{ universe => runtimeUniverse }

import ScalaModel._

final class ScalaParserSpec extends org.specs2.mutable.Specification {
  "Scala parser" title

  import ScalaParserResults._
  import ScalaRuntimeFixtures._
  import runtimeUniverse.EmptyTree

  private implicit def cl: ClassLoader = getClass.getClassLoader

  val scalaParser = new ScalaParser[runtimeUniverse.type](
    universe = runtimeUniverse,
    logger = Logger(org.slf4j.LoggerFactory getLogger "ScalaParserSpec"))

  "Parser" should {
    "handle case class with one primitive member" in {
      val res = scalaParser.parseTypes(
        List(TestClass1Type -> TestClass1Tree),
        Map.empty,
        ListSet.empty)

      res.parsed must contain(caseClass1) and {
        res.parsed must have size 1
      }
    }

    "handle generic case class with one member" in {
      val res = scalaParser.parseTypes(
        List(TestClass2Type -> TestClass2Tree),
        Map.empty,
        ListSet.empty)

      res.parsed must contain(caseClass2) and {
        res.parsed must have size 1
      }
    }

    "handle generic case class with one member list of type parameter" in {
      val res = scalaParser.parseTypes(
        List(TestClass3Type -> TestClass3Tree),
        Map.empty,
        ListSet.empty)

      res.parsed must contain(caseClass3) and {
        res.parsed must have size 1
      }
    }

    "handle generic case class with one optional member" in {
      val res = scalaParser.parseTypes(
        List(TestClass5Type -> TestClass5Tree),
        Map.empty,
        ListSet.empty)

      res.parsed must contain(caseClass5) and {
        res.parsed must have size 1
      }
    }

    "detect involved types and skipped already examined types" in {
      /*
      {
        import scala.tools.reflect._
        import runtimeUniverse._

        val tb = runtimeUniverse.rootMirror.mkToolBox()

      }
       */

      val res = scalaParser.parseTypes(
        List(
          TestClass6Type -> TestClass6Tree,
          TestClass4Type -> TestClass4Tree, // skipped as examined from 6
          TestClass2Type -> TestClass2Tree, // skipped as examined from 6
          TestClass1Type -> TestClass1Tree // skipped as examined from 6
        ),
        Map(
          TestClass3Type.typeSymbol. // 'age' in 6
            fullName -> (TestClass3Type -> TestClass3Tree),
          TestClass5Type.typeSymbol. // 'age' in 6
            fullName -> (TestClass5Type -> TestClass5Tree)),
        ListSet.empty)

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
        List(TestClass7Type -> TestClass7Tree),
        Map(
          TestClass1Type.typeSymbol. // 'name' in 7
            fullName -> (TestClass1Type -> TestClass1Tree),
          TestClass1BType.typeSymbol. // 'name' in 7
            fullName -> (TestClass1BType -> TestClass1BTree)),
        ListSet.empty)

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
        List(AnyValChildType -> AnyValChildTree),
        Map.empty,
        ListSet.empty)

      res.parsed must beEmpty
    }

    "handle ValueClass member as a primitive type" in {
      val res = scalaParser.parseTypes(
        List(TestClass8Type -> TestClass8Tree),
        Map.empty,
        ListSet.empty)

      res.parsed must contain(caseClass8) and {
        res.parsed must have size 1
      }
    }

    "handle enumeration" >> {
      "type declaration" in {
        val res = scalaParser.parseTypes(
          List(TestEnumerationType -> TestEnumerationTree),
          Map.empty,
          ListSet.empty)

        res.parsed must contain(testEnumeration) and {
          res.parsed must have size 1
        }
      }

      "as member in class" in {
        val res = scalaParser.parseTypes(
          List(TestClass9Type -> TestClass9Tree),
          Map.empty,
          ListSet.empty)

        res.parsed must contain(caseClass9) and {
          res.parsed must contain(testEnumeration)
        } and {
          res.parsed must have size 2
        }
      }
    }

    "handle tuple values" in {
      val res = scalaParser.parseTypes(
        List(TestClass10Type -> TestClass10Tree),
        Map.empty,
        ListSet.empty)

      res.parsed must contain(caseClass10) and {
        res.parsed must have size 1
      }
    }

    "handle object" >> {
      "from case object" in {
        val res = scalaParser.parseTypes(
          List(ScalaRuntimeFixtures.TestObject1Type -> EmptyTree),
          Map.empty,
          ListSet.empty)

        res.parsed must contain(caseObject1) and {
          res.parsed must have size 1
        }
      }

      "skip when companion object" in {
        val res = scalaParser.parseTypes(
          List(TestClass1CompanionType -> TestClass1CompanionTree),
          Map.empty,
          ListSet.empty)

        res.parsed must beEmpty
      }

      "from plain object with values" in {
        val res = scalaParser.parseTypes(
          List(TestObject2Type -> TestObject2Tree),
          Map.empty,
          ListSet.empty)

        res.parsed must contain(caseObject2) and {
          res.parsed must have size 1
        }
      }
    }

    "handle sealed trait as union" in {
      val res = scalaParser.parseTypes(
        List(FamilyType -> FamilyTree),
        Map(
          FamilyMember1Type.typeSymbol.
            fullName -> (FamilyMember1Type -> FamilyMember1Tree),
          FamilyMember2Type.typeSymbol.
            fullName -> (FamilyMember2Type -> FamilyMember2Tree),
          FamilyMember3Type.typeSymbol.
            fullName -> (FamilyMember3Type -> FamilyMember3Tree)),
        ListSet.empty)

      res.parsed must contain(sealedFamily1) and {
        res.parsed must have size 1
      }
    }
  }
}

object ScalaRuntimeFixtures {
  implicit val mirror = runtimeUniverse.runtimeMirror(getClass.getClassLoader)
  import runtimeUniverse._

  private lazy val tb = {
    import scala.tools.reflect._

    runtimeUniverse.rootMirror.mkToolBox()
  }

  @inline private def retry[T](n: Int)(f: => T): T = try {
    f
  } catch {
    case NonFatal(_) if (n > 0) =>
      retry(n - 1)(f)

    case NonFatal(cause) =>
      throw cause
  }

  @inline private def typecheck(tree: Tree) =
    retry(5)(tb.typecheck(tree))

  case class TestClass1(name: String)

  val TestClass1Type = typeOf[TestClass1]

  lazy val TestClass1Tree: Tree =
    typecheck(q"case class TestClass1(name: String)")

  val TestClass1CompanionType = typeOf[TestClass1.type]

  lazy val TestClass1CompanionTree: Tree = typecheck(q"object TestClass1 {}")

  case class TestClass1B(foo: String)

  val TestClass1BType = typeOf[TestClass1B]

  lazy val TestClass1BTree: Tree =
    typecheck(q"case class TestClass1B(foo: String)")

  case class TestClass2[T](name: T)

  val TestClass2Type = typeOf[TestClass2[_]]

  lazy val TestClass2Tree: Tree = typecheck(
    q"case class TestClass2[T](name: T)")

  case class TestClass3[T](name: List[T])

  val TestClass3Type = typeOf[TestClass3[_]]

  lazy val TestClass3Tree: Tree = typecheck(
    q"case class TestClass3[T](name: List[T])")

  case class TestClass4[T](name: TestClass3[T])

  val TestClass4Type = typeOf[TestClass4[_]]

  lazy val TestClass4Tree: Tree =
    q"case class TestClass4[T](name: TestClass3[T])"

  case class TestClass5[T](
    name: Option[T],
    counters: Map[String, java.math.BigInteger])

  val TestClass5Type = typeOf[TestClass5[_]]

  lazy val TestClass5Tree: Tree = typecheck(q"""case class TestClass5[T](
    name: Option[T],
    counters: Map[String, java.math.BigInteger])""")

  case class TestClass6[T](
    name: Option[TestClass5[List[Option[TestClass4[String]]]]],
    age: TestClass3[TestClass2[TestClass1]])

  val TestClass6Type = typeOf[TestClass6[_]]

  lazy val TestClass6Tree: Tree = q"""case class TestClass6[T](
    name: Option[TestClass5[List[Option[TestClass4[String]]]]],
    age: TestClass3[TestClass2[TestClass1]])"""

  case class TestClass7[T](name: Either[TestClass1, TestClass1B])

  val TestClass7Type = typeOf[TestClass7[_]]

  lazy val TestClass7Tree: Tree = q"""case class TestClass7[T](
    name: Either[TestClass1, TestClass1B])"""

  case class AnyValChild(value: String) extends AnyVal

  val AnyValChildType = typeOf[AnyValChild]

  lazy val AnyValChildTree: Tree = typecheck(
    q"case class AnyValChild(value: String)")

  case class TestClass8(name: AnyValChild)

  val TestClass8Type = typeOf[TestClass8]
  val TestClass8Tree: Tree = q"case class TestClass8(name: AnyValChild)"

  object TestEnumeration extends scala.Enumeration {
    val A, B, C = Value
  }

  val TestEnumerationType = typeOf[TestEnumeration.type]

  lazy val TestEnumerationTree: Tree = typecheck(
    q"""object TestEnumeration extends scala.Enumeration {
    val A, B, C = Value
  }""")

  case class TestClass9(name: TestEnumeration.Value)

  val TestClass9Type = typeOf[TestClass9]

  lazy val TestClass9Tree: Tree =
    q"case class TestClass9(name: TestEnumeration.Value)"

  case class TestClass10(
    name: String,
    tuple: Tuple1[Int],
    tupleA: (String, Int),
    tupleB: Tuple2[String, Long],
    tupleC: Tuple3[String, String, Long])

  val TestClass10Type = typeOf[TestClass10]

  lazy val TestClass10Tree: Tree = typecheck(q"""case class TestClass10(
    name: String,
    tuple: Tuple1[Int],
    tupleA: (String, Int),
    tupleB: Tuple2[String, Long],
    tupleC: Tuple3[String, String, Long])""")

  case object TestObject1

  val TestObject1Type = typeOf[TestObject1.type]

  lazy val TestObject1Tree: Tree = typecheck(q"case object TestObject1")

  object TestObject2 {
    val name = "Foo"
    def code = 1
  }

  val TestObject2Type = typeOf[TestObject2.type]

  lazy val TestObject2Tree: Tree = typecheck(q"""
    class Foo(val name: String)

    object TestObject2 extends Foo("Foo") {
      def code = 1
    }""").children.drop(1).head

  sealed trait Family {
    def foo: String
    val bar = "lorem"
    def ipsum = 0.1D
  }

  val FamilyType = typeOf[Family]

  lazy val FamilyTree: Tree = typecheck(q"""trait Family {
    def foo: String
    val bar = "lorem"
    def ipsum = 0.1D
  }""")

  case class FamilyMember1(foo: String) extends Family {
    val code = 1
  }

  val FamilyMember1Type = typeOf[FamilyMember1]

  lazy val FamilyMember1Tree: Tree = typecheck(
    q"""${tb untypecheck FamilyTree}; case class FamilyMember1(foo: String) extends Family {
      val code = 1
    }""")

  case object FamilyMember2 extends Family {
    // Members are unsupported for object,
    // and so the TS singleton won't implements the common interface
    val foo = "bar"
  }

  val FamilyMember2Type = typeOf[FamilyMember2.type]

  lazy val FamilyMember2Tree: Tree = typecheck(
    q"""${tb untypecheck FamilyTree}; object FamilyMember2 extends Family {
      val foo = "bar"
    }""")

  object FamilyMember3 extends Family {
    def foo = "lorem"
  }

  val FamilyMember3Type = typeOf[FamilyMember3.type]

  lazy val FamilyMember3Tree: Tree = typecheck(
    q"""${tb untypecheck FamilyTree}; object FamilyMember3 extends Family {
      def foo = "lorem"
    }""")
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
    fields = ListSet(TypeMember("name", CollectionRef(TypeParamRef("T")))),
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
          typeArgs = List(CollectionRef(OptionRef(
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

  val caseObject2 = CaseObject(
    QualifiedIdentifier(
      "TestObject2", List("ScalaRuntimeFixtures")),
    ListSet(
      TypeInvariant("name", StringRef, "\"Foo\""),
      TypeInvariant("code", IntRef, "1")))

  val sealedFamily1 = {
    // Not 'bar', as not abstract
    val fooMember = TypeMember("foo", StringRef)
    val fooBar = TypeInvariant("foo", StringRef, "\"bar\"")
    val fooLorem = TypeInvariant("foo", StringRef, "\"lorem\"")
    val code = TypeInvariant("code", IntRef, "1")

    SealedUnion(
      QualifiedIdentifier("Family", List("ScalaRuntimeFixtures")),
      ListSet(fooMember),
      ListSet(
        CaseClass(
          identifier = QualifiedIdentifier(
            "FamilyMember1", List("ScalaRuntimeFixtures")),
          fields = ListSet(fooMember),
          values = ListSet(code),
          typeArgs = List.empty),
        CaseObject(QualifiedIdentifier(
          "FamilyMember2", List("ScalaRuntimeFixtures")), ListSet(fooBar)),
        CaseObject(QualifiedIdentifier(
          "FamilyMember3", List("ScalaRuntimeFixtures")), ListSet(fooLorem))))
  }
}
