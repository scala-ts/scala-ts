package io.github.scalats.core

import scala.util.control.NonFatal

import scala.reflect.runtime.{ universe => runtimeUniverse }

import io.github.scalats.core.Internals.ListSet
import io.github.scalats.scala._

final class ScalaParserSpec extends org.specs2.mutable.Specification {
  "Scala parser" title

  import ScalaParserResults._
  import ScalaRuntimeFixtures._
  import runtimeUniverse.{ Type, Tree, EmptyTree }

  private implicit def cl: ClassLoader = getClass.getClassLoader

  "Parser" should {
    "handle case class with one primitive member" in {
      val res = parseTypes(
        List(TestClass1Type -> TestClass1Tree)
      )

      res must contain(caseClass1) and {
        res must have size 1
      }
    }

    "handle generic case class with one member" in {
      val res = parseTypes(
        List(TestClass2Type -> TestClass2Tree)
      )

      res must contain(caseClass2) and {
        res must have size 1
      }
    }

    "handle generic case class with one member list of type parameter" in {
      val res = parseTypes(
        List(TestClass3Type -> TestClass3Tree)
      )

      res must contain(caseClass3) and {
        res must have size 1
      }
    }

    "handle generic case class with one optional member" in {
      val res = parseTypes(
        List(TestClass5Type -> TestClass5Tree)
      )

      res must contain(caseClass5) and {
        res must have size 1
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

      val res = parseTypes(
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
          fullName -> (TestClass5Type -> TestClass5Tree)
        )
      )

      res must contain(caseClass1) and {
        res must contain(caseClass2)
      } and {
        res must contain(caseClass3)
      } and {
        res must contain(caseClass4)
      } and {
        res must contain(caseClass5)
      } and {
        res must contain(caseClass6)
      } and {
        res must have size 6
      }
    }

    "handle either types" in {
      val res = parseTypes(
        List(TestClass7Type -> TestClass7Tree),
        Map(
          TestClass1Type.typeSymbol. // 'name' in 7
          fullName -> (TestClass1Type -> TestClass1Tree),
          TestClass1BType.typeSymbol. // 'name' in 7
          fullName -> (TestClass1BType -> TestClass1BTree)
        )
      )

      res must contain(caseClass7) and {
        res must contain(caseClass1)
      } and {
        res must contain(caseClass1B)
      } and {
        res must have size 3
      }
    }

    "handle ValueClass" >> {
      "declaration" in {
        val res = parseTypes(
          List(AnyValChildType -> AnyValChildTree)
        )

        res must_=== List(tagged1)
      }

      "as member" in {
        val res = parseTypes(
          List(TestClass8Type -> TestClass8Tree)
        )

        res must contain(caseClass8) and {
          res must contain(tagged1)
        } and {
          res must have size 2
        }
      }
    }

    "handle enumeration" >> {
      "type declaration" in {
        val res = parseTypes(
          List(TestEnumerationType -> TestEnumerationTree)
        )

        res must contain(testEnumeration) and {
          res must have size 1
        }
      }

      "as member in class" in {
        val res = parseTypes(
          List(TestClass9Type -> TestClass9Tree)
        )

        res must contain(caseClass9) and {
          res must contain(testEnumeration)
        } and {
          res must have size 2
        }
      }
    }

    "handle tuple values" in {
      val res = parseTypes(
        List(TestClass10Type -> TestClass10Tree)
      )

      res must contain(caseClass10) and {
        res must have size 1
      }
    }

    "handle object" >> {
      "from case object" in {
        val res = parseTypes(
          List(ScalaRuntimeFixtures.TestObject1Type -> EmptyTree)
        )

        res must contain(caseObject1) and {
          res must have size 1
        }
      }

      "skip when companion object" in {
        val res = parseTypes(
          List(TestClass1CompanionType -> TestClass1CompanionTree)
        )

        res must beEmpty
      }

      "from plain object with values" in {
        val res = parseTypes(
          List(TestObject2Type -> TestObject2Tree)
        )

        res must contain(caseObject2) and {
          res must have size 1
        }
      } tag "wip"
    }

    "handle sealed trait as union" in {
      val res = parseTypes(
        List(FamilyType -> FamilyTree),
        Map(
          FamilyMember1Type.typeSymbol.fullName -> (FamilyMember1Type -> FamilyMember1Tree),
          FamilyMember2Type.typeSymbol.fullName -> (FamilyMember2Type -> FamilyMember2Tree),
          FamilyMember3Type.typeSymbol.fullName -> (FamilyMember3Type -> FamilyMember3Tree)
        )
      )

      res must contain(sealedFamily1) and {
        res must have size 1
      }
    }
  }

  // ---

  private val scalaParser = new ScalaParser[runtimeUniverse.type](
    universe = runtimeUniverse,
    compiled = Set.empty,
    logger = Logger(org.slf4j.LoggerFactory getLogger "ScalaParserSpec")
  )

  private def parseTypes(
      types: List[(Type, Tree)],
      symtab: Map[String, (Type, Tree)] = Map.empty
    ): List[TypeDef] = scalaParser
    .parseTypes(
      types,
      symtab,
      ListSet.empty,
      _ => true
    )
    .parsed
    .toList
}

object ScalaRuntimeFixtures {
  implicit val mirror = runtimeUniverse.runtimeMirror(getClass.getClassLoader)
  import runtimeUniverse._

  private lazy val tb = {
    import scala.tools.reflect._

    runtimeUniverse.rootMirror.mkToolBox()
  }

  @inline private def retry[T](n: Int)(f: => T): T =
    try {
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
    q"case class TestClass2[T](name: T)"
  )

  case class TestClass3[T](name: List[T])

  val TestClass3Type = typeOf[TestClass3[_]]

  lazy val TestClass3Tree: Tree = typecheck(
    q"case class TestClass3[T](name: List[T])"
  )

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
    q"case class AnyValChild(value: String)"
  )

  case class TestClass8(
      name: AnyValChild,
      aliases: Seq[AnyValChild])

  val TestClass8Type = typeOf[TestClass8]

  val TestClass8Tree: Tree = q"""case class TestClass8(
    name: AnyValChild, aliases: Seq[AnyValChild])"""

  object TestEnumeration extends scala.Enumeration {
    val A, B, C = Value
  }

  val TestEnumerationType = typeOf[TestEnumeration.type]

  lazy val TestEnumerationTree: Tree = typecheck(
    q"""object TestEnumeration extends scala.Enumeration {
    val A, B, C = Value
  }"""
  )

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
    val const = new String("value")
    def foo = name

    val list = Seq("first", name)
    def set: Set[Int] = Set(code, 2)
    val mapping = Map("foo" -> "bar", (new String("lorem")) -> name)

    def dictOfList = Map(
      new String("excludes") -> Seq("*.txt", ".gitignore"),
      "includes" -> Seq("images/**", "*.jpg", "*.png")
    )

    val concatSeq = list ++ Seq("foo", "bar") ++ Seq("lorem")
    def concatList = List("foo") ++ list

    val mergedSet = set ++ Set(3)
  }

  val TestObject2Type = typeOf[TestObject2.type]

  lazy val TestObject2Tree: Tree = typecheck(q"""
    class Foo(val name: String)

    object TestObject2 extends Foo("Foo") {
      def code = 1
      val const = new String("value")
      def foo = name

      val list = Seq("first", name)
      def set: Set[Int] = Set(code, 2)
      val mapping = Map("foo" -> "bar", (new String("lorem")) -> name)

      def dictOfList = Map(
        new String("excludes") -> Seq("*.txt", ".gitignore"),
        "includes" -> Seq("images/**", "*.jpg", "*.png"))

      val concatSeq = list ++ Seq("foo", "bar") ++ Seq("lorem")
      def concatList = List("foo") ++ list

      val mergedSet = set ++ Set(3)
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
    }"""
  )

  case object FamilyMember2 extends Family {
    // Members are unsupported for object,
    // and so the TS singleton won't implements the common interface
    val foo = "bar"
  }

  val FamilyMember2Type = typeOf[FamilyMember2.type]

  lazy val FamilyMember2Tree: Tree = typecheck(
    q"""${tb untypecheck FamilyTree}; object FamilyMember2 extends Family {
      val foo = "bar"
    }"""
  )

  object FamilyMember3 extends Family {
    def foo = "lorem"
  }

  val FamilyMember3Type = typeOf[FamilyMember3.type]

  lazy val FamilyMember3Tree: Tree = typecheck(
    q"""${tb untypecheck FamilyTree}; object FamilyMember3 extends Family {
      def foo = "lorem"
    }"""
  )
}

object ScalaParserResults {

  val caseClass1 = CaseClass(
    identifier =
      QualifiedIdentifier("TestClass1", List("ScalaRuntimeFixtures")),
    fields = ListSet(TypeMember("name", StringRef)),
    values = ListSet.empty,
    typeArgs = List.empty
  )

  val caseClass1B = CaseClass(
    identifier =
      QualifiedIdentifier("TestClass1B", List("ScalaRuntimeFixtures")),
    fields = ListSet(TypeMember("foo", StringRef)),
    values = ListSet.empty,
    typeArgs = List.empty
  )

  val caseClass2 = CaseClass(
    identifier =
      QualifiedIdentifier("TestClass2", List("ScalaRuntimeFixtures")),
    fields = ListSet(TypeMember("name", TypeParamRef("T"))),
    values = ListSet.empty,
    typeArgs = List("T")
  )

  val caseClass3 = CaseClass(
    identifier =
      QualifiedIdentifier("TestClass3", List("ScalaRuntimeFixtures")),
    fields = ListSet(TypeMember("name", CollectionRef(TypeParamRef("T")))),
    values = ListSet.empty,
    typeArgs = List("T")
  )

  val caseClass4 = CaseClass(
    identifier =
      QualifiedIdentifier("TestClass4", List("ScalaRuntimeFixtures")),
    fields = ListSet(
      TypeMember(
        "name",
        CaseClassRef(
          QualifiedIdentifier("TestClass3", List("ScalaRuntimeFixtures")),
          typeArgs = List(TypeParamRef("T"))
        )
      )
    ),
    values = ListSet.empty,
    typeArgs = List("T")
  )

  val caseClass5 = CaseClass(
    identifier =
      QualifiedIdentifier("TestClass5", List("ScalaRuntimeFixtures")),
    fields = ListSet(
      TypeMember("name", OptionRef(TypeParamRef("T"))),
      TypeMember("counters", MapRef(StringRef, BigIntegerRef))
    ),
    values = ListSet.empty,
    typeArgs = List("T")
  )

  val caseClass6 = CaseClass(
    identifier =
      QualifiedIdentifier("TestClass6", List("ScalaRuntimeFixtures")),
    fields = ListSet(
      TypeMember(
        "name",
        OptionRef(
          CaseClassRef(
            QualifiedIdentifier("TestClass5", List("ScalaRuntimeFixtures")),
            typeArgs = List(
              CollectionRef(
                OptionRef(
                  CaseClassRef(
                    QualifiedIdentifier(
                      "TestClass4",
                      List("ScalaRuntimeFixtures")
                    ),
                    typeArgs = List(StringRef)
                  )
                )
              )
            )
          )
        )
      ),
      TypeMember(
        "age",
        CaseClassRef(
          QualifiedIdentifier("TestClass3", List("ScalaRuntimeFixtures")),
          typeArgs = List(
            CaseClassRef(
              QualifiedIdentifier("TestClass2", List("ScalaRuntimeFixtures")),
              typeArgs = List(
                CaseClassRef(
                  QualifiedIdentifier(
                    "TestClass1",
                    List("ScalaRuntimeFixtures")
                  ),
                  typeArgs = List.empty
                )
              )
            )
          )
        )
      )
    ),
    values = ListSet.empty,
    typeArgs = List("T")
  )

  val caseClass7 = CaseClass(
    identifier =
      QualifiedIdentifier("TestClass7", List("ScalaRuntimeFixtures")),
    fields = ListSet(
      TypeMember(
        "name",
        UnionRef(
          ListSet(
            CaseClassRef(
              QualifiedIdentifier("TestClass1", List("ScalaRuntimeFixtures")),
              typeArgs = List.empty
            ),
            CaseClassRef(
              QualifiedIdentifier("TestClass1B", List("ScalaRuntimeFixtures")),
              typeArgs = List.empty
            )
          )
        )
      )
    ),
    values = ListSet.empty,
    typeArgs = List("T")
  )

  val tagged1 = ValueClass(
    identifier =
      QualifiedIdentifier("AnyValChild", List("ScalaRuntimeFixtures")),
    field = TypeMember("value", StringRef)
  )

  val caseClass8 = CaseClass(
    identifier =
      QualifiedIdentifier("TestClass8", List("ScalaRuntimeFixtures")),
    fields = ListSet(
      TypeMember(
        "name",
        TaggedRef(
          identifier =
            QualifiedIdentifier("AnyValChild", List("ScalaRuntimeFixtures")),
          tagged = StringRef
        )
      ),
      TypeMember(
        "aliases",
        CollectionRef(
          TaggedRef(
            identifier =
              QualifiedIdentifier("AnyValChild", List("ScalaRuntimeFixtures")),
            tagged = StringRef
          )
        )
      )
    ),
    values = ListSet.empty,
    typeArgs = List.empty
  )

  val caseClass9 = CaseClass(
    identifier =
      QualifiedIdentifier("TestClass9", List("ScalaRuntimeFixtures")),
    fields = ListSet(
      TypeMember(
        "name",
        EnumerationRef(
          QualifiedIdentifier("TestEnumeration", List("ScalaRuntimeFixtures"))
        )
      )
    ),
    values = ListSet.empty,
    typeArgs = List.empty
  )

  val testEnumeration = EnumerationDef(
    QualifiedIdentifier("TestEnumeration", List("ScalaRuntimeFixtures")),
    ListSet("A", "B", "C")
  )

  val caseClass10 = CaseClass(
    identifier =
      QualifiedIdentifier("TestClass10", List("ScalaRuntimeFixtures")),
    fields = ListSet(
      TypeMember("name", StringRef),
      TypeMember("tuple", TupleRef(List(IntRef))),
      TypeMember("tupleA", TupleRef(List(StringRef, IntRef))),
      TypeMember("tupleB", TupleRef(List(StringRef, LongRef))),
      TypeMember("tupleC", TupleRef(List(StringRef, StringRef, LongRef)))
    ),
    values = ListSet.empty,
    typeArgs = List.empty
  )

  val caseObject1 = CaseObject(
    QualifiedIdentifier("TestObject1", List("ScalaRuntimeFixtures")),
    ListSet.empty
  )

  val caseObject2 = CaseObject(
    QualifiedIdentifier("TestObject2", List("ScalaRuntimeFixtures")),
    ListSet(
      LiteralInvariant("name", StringRef, "\"Foo\""),
      LiteralInvariant("code", IntRef, "1"),
      LiteralInvariant("const", StringRef, "\"value\""),
      SelectInvariant("foo", StringRef, ThisTypeRef, "name"),
      ListInvariant(
        name = "list",
        typeRef = CollectionRef(StringRef),
        valueTypeRef = StringRef,
        values = List(
          LiteralInvariant("list[0]", StringRef, "\"first\""),
          SelectInvariant("list[1]", StringRef, ThisTypeRef, "name")
        )
      ),
      SetInvariant(
        name = "set",
        typeRef = CollectionRef(IntRef),
        valueTypeRef = IntRef,
        values = Set(
          SelectInvariant("set[0]", IntRef, ThisTypeRef, "code"),
          LiteralInvariant("set[1]", IntRef, "2")
        )
      ),
      DictionaryInvariant(
        name = "mapping",
        keyTypeRef = StringRef,
        valueTypeRef = StringRef,
        entries = Map(
          LiteralInvariant(
            "mapping.0",
            StringRef,
            "\"foo\""
          ) -> LiteralInvariant("mapping[0]", StringRef, "\"bar\""),
          LiteralInvariant(
            "mapping.1",
            StringRef,
            "\"lorem\""
          ) -> SelectInvariant(
            "mapping[1]",
            StringRef,
            ThisTypeRef,
            "name"
          )
        )
      ),
      DictionaryInvariant(
        name = "dictOfList",
        keyTypeRef = StringRef,
        valueTypeRef = CollectionRef(StringRef),
        entries = Map(
          LiteralInvariant(
            "dictOfList.0",
            StringRef,
            "\"excludes\""
          ) -> ListInvariant(
            "dictOfList[0]",
            CollectionRef(StringRef),
            StringRef,
            List(
              LiteralInvariant(
                "dictOfList[0][0]",
                StringRef,
                "\"*.txt\""
              ),
              LiteralInvariant(
                "dictOfList[0][1]",
                StringRef,
                "\".gitignore\""
              )
            )
          ),
          LiteralInvariant(
            "dictOfList.1",
            StringRef,
            "\"includes\""
          ) -> ListInvariant(
            "dictOfList[1]",
            CollectionRef(StringRef),
            StringRef,
            List(
              LiteralInvariant(
                "dictOfList[1][0]",
                StringRef,
                "\"images/**\""
              ),
              LiteralInvariant(
                "dictOfList[1][1]",
                StringRef,
                "\"*.jpg\""
              ),
              LiteralInvariant(
                "dictOfList[1][2]",
                StringRef,
                "\"*.png\""
              )
            )
          )
        )
      ),
      MergedListsInvariant(
        name = "concatSeq",
        valueTypeRef = StringRef,
        children = List(
          SelectInvariant(
            name = "concatSeq[0]",
            typeRef = CollectionRef(StringRef),
            qualifier = ThisTypeRef,
            term = "list"
          ),
          ListInvariant(
            name = "concatSeq[1]",
            typeRef = CollectionRef(StringRef),
            valueTypeRef = StringRef,
            values = List(
              LiteralInvariant("concatSeq[1][0]", StringRef, "\"foo\""),
              LiteralInvariant("concatSeq[1][1]", StringRef, "\"bar\"")
            )
          ),
          ListInvariant(
            name = "concatSeq[2]",
            typeRef = CollectionRef(StringRef),
            valueTypeRef = StringRef,
            values =
              List(LiteralInvariant("concatSeq[2][0]", StringRef, "\"lorem\""))
          )
        )
      ),
      MergedListsInvariant(
        name = "concatList",
        valueTypeRef = StringRef,
        children = List(
          ListInvariant(
            name = "concatList[0]",
            typeRef = CollectionRef(StringRef),
            valueTypeRef = StringRef,
            values =
              List(LiteralInvariant("concatList[0][0]", StringRef, "\"foo\""))
          ),
          SelectInvariant(
            name = "concatList[1]",
            typeRef = CollectionRef(StringRef),
            qualifier = ThisTypeRef,
            term = "list"
          )
        )
      ),
      MergedSetsInvariant(
        name = "mergedSet",
        valueTypeRef = IntRef,
        children = List(
          SelectInvariant(
            name = "mergedSet[0]",
            typeRef = CollectionRef(IntRef),
            qualifier = ThisTypeRef,
            term = "set"
          ),
          SetInvariant(
            name = "mergedSet[1]",
            typeRef = CollectionRef(IntRef),
            valueTypeRef = IntRef,
            values = Set(LiteralInvariant("mergedSet[1][0]", IntRef, "3"))
          )
        )
      )
    )
  )

  val sealedFamily1 = {
    // Not 'bar', as not abstract
    val fooMember = TypeMember("foo", StringRef)
    val fooBar = LiteralInvariant("foo", StringRef, "\"bar\"")
    val fooLorem = LiteralInvariant("foo", StringRef, "\"lorem\"")
    val code = LiteralInvariant("code", IntRef, "1")

    SealedUnion(
      QualifiedIdentifier("Family", List("ScalaRuntimeFixtures")),
      ListSet(fooMember),
      ListSet(
        CaseClass(
          identifier =
            QualifiedIdentifier("FamilyMember1", List("ScalaRuntimeFixtures")),
          fields = ListSet(fooMember),
          values = ListSet(code),
          typeArgs = List.empty
        ),
        CaseObject(
          QualifiedIdentifier("FamilyMember2", List("ScalaRuntimeFixtures")),
          ListSet(fooBar)
        ),
        CaseObject(
          QualifiedIdentifier("FamilyMember3", List("ScalaRuntimeFixtures")),
          ListSet(fooLorem)
        )
      )
    )
  }
}
