package io.github.scalats.core

import io.github.scalats.core.Internals.ListSet
import io.github.scalats.scala.TypeDef

// TODO: Multiple output
final class ScalaParserSpec
    extends org.specs2.mutable.Specification
    with ScalaExtraParserSpec {

  "Scala parser".title

  import ScalaRuntimeFixtures._, results._

  "Parser" should {
    "handle case class with one primitive member" in {
      val res = parseTypes(
        List(TestClass1Type -> TestClass1Tree)
      )

      res must_=== List(caseClass1.identifier.name -> ListSet(caseClass1))
    }

    "handle generic case class with one member" in {
      val res = parseTypes(
        List(TestClass2Type -> TestClass2Tree)
      )

      res must_=== List(caseClass2.identifier.name -> ListSet(caseClass2))
    }

    "handle generic case class with one member list of type parameter" in {
      val res = parseTypes(
        List(TestClass3Type -> TestClass3Tree)
      )

      res must_=== List(caseClass3.identifier.name -> ListSet(caseClass3))
    }

    "handle generic case class with one optional member" in {
      val res = parseTypes(
        List(TestClass5Type -> TestClass5Tree)
      )

      res must_=== List(caseClass5.identifier.name -> ListSet(caseClass5))
    }

    "detect involved types and skipped already examined types" in {
      val res = parseTypes(
        List(
          TestClass6Type -> TestClass6Tree,
          TestClass4Type -> TestClass4Tree, // skipped as examined from 6
          TestClass2Type -> TestClass2Tree, // skipped as examined from 6
          TestClass1Type -> TestClass1Tree // skipped as examined from 6
        ),
        Map(
          fullName(
            TestClass3Type.typeSymbol // 'age' in 6
          ) -> ListSet(TestClass3Type -> TestClass3Tree),
          fullName(
            TestClass5Type.typeSymbol // 'age' in 6
          ) -> ListSet(TestClass5Type -> TestClass5Tree)
        )
      )

      res must have size 6 and {
        res must contain(caseClass1.identifier.name -> ListSet(caseClass1))
      } and {
        res must contain(caseClass2.identifier.name -> ListSet(caseClass2))
      } and {
        res must contain(caseClass3.identifier.name -> ListSet(caseClass3))
      } and {
        res must contain(caseClass4.identifier.name -> ListSet(caseClass4))
      } and {
        res must contain(caseClass6.identifier.name -> ListSet(caseClass6))
      } and {
        // as required for a caseClass6 property
        res must contain(caseClass5.identifier.name -> ListSet(caseClass5))
      }
    }

    "handle either types" in {
      val res = parseTypes(
        List(TestClass7Type -> TestClass7Tree),
        Map(
          fullName(
            TestClass1Type.typeSymbol // 'name' in 7
          ) -> ListSet(TestClass1Type -> TestClass1Tree),
          fullName(
            TestClass1BType.typeSymbol // 'name' in 7
          ) -> ListSet(TestClass1BType -> TestClass1BTree)
        )
      )

      res must have size 3 and {
        res must contain(caseClass7.identifier.name -> ListSet(caseClass7))
      } and {
        res must contain(caseClass1.identifier.name -> ListSet(caseClass1))
      } and {
        res must contain(caseClass1B.identifier.name -> ListSet(caseClass1B))
      }
    }

    "handle ValueClass" >> {
      "declaration" in eventually {
        val res = parseTypes(
          List(AnyValChildType -> AnyValChildTree)
        )

        res must_=== List(tagged1.identifier.name -> ListSet(tagged1))
      }

      "as member" in {
        val res = parseTypes(
          List(TestClass8Type -> TestClass8Tree)
        )

        res must have size 2 and {
          res must contain(caseClass8.identifier.name -> ListSet(caseClass8))
        } and {
          res must contain(tagged1.identifier.name -> ListSet(tagged1))
        }
      }
    }

    "handle enumeration" >> {
      "type declaration" in {
        val res = parseTypes(
          List(TestEnumerationType -> TestEnumerationTree)
        )

        res must_=== List(
          testEnumeration.identifier.name -> ListSet(testEnumeration)
        )
      }

      "as member in class" in {
        val res = parseTypes(
          List(TestClass9Type -> TestClass9Tree)
        )

        res must contain(
          caseClass9.identifier.name -> ListSet(caseClass9)
        ) and {
          res must contain(
            testEnumeration.identifier.name -> ListSet(testEnumeration)
          )
        } and {
          res must have size 2
        }
      }
    }

    "handle tuple values" in {
      val res = parseTypes(
        List(TestClass10Type -> TestClass10Tree)
      )

      res must_=== List(
        caseClass10.identifier.name -> ListSet(caseClass10)
      )
    }

    "handle object" >> {
      "from case object" in {
        // TODO: Now empty object is filtered out but...
        val tpe = ScalaRuntimeFixtures.TestObject1Type -> EmptyTree
        val expected =
          List(caseObject1.identifier.name -> ListSet[TypeDef](caseObject1))

        val res1 = parseType(
          tpe,
          symtab = Map.empty,
          examined = ListSet.empty,
          acceptsType = _ => true
        )

        res1.parsed.toList must_=== expected and {
          // Make sure only the object is marked as examined
          // (not its associated types; e.g. class)

          res1.examined must_=== ListSet(
            ScalaRuntimeFixtures.objectClass("TestObject1") + '#'
          )
        } and {
          val res2 = parseTypes(List(tpe))

          res2 must_=== expected
        }
      }

      "from plain object with values" in {
        val res = parseTypes(
          List(TestObject2Type -> TestObject2Tree)
        )

        res must_=== List(
          caseObject2.identifier.name -> ListSet(caseObject2),
          nestedObj1.identifier.name -> ListSet(nestedObj1)
        )
      }
    }

    "handle sealed trait as union" in {
      val res = parseTypes(
        List(FamilyType -> FamilyTree),
        Map(
          fullName(
            FamilyMember1Type.typeSymbol
          ) -> ListSet(FamilyMember1Type -> FamilyMember1Tree),
          fullName(
            FamilyMember2Type.typeSymbol
          ) -> ListSet(FamilyMember2Type -> FamilyMember2Tree),
          fullName(
            FamilyMember3Type.typeSymbol
          ) -> ListSet(FamilyMember3Type -> FamilyMember3Tree)
        )
      )

      res must_=== List(sealedFamily1.identifier.name -> ListSet(sealedFamily1))
    }

    "resolve data type" >> {
      import io.github.scalats.scala.{
        UnknownTypeRef,
        CaseClassRef,
        QualifiedIdentifier
      }

      "for TestClass8" in {
        scalaParser.dataTypeRef(TestClass1Type) must beSome(
          CaseClassRef(
            QualifiedIdentifier("TestClass1", results.ns),
            List.empty
          )
        )
      }

      "for type refinement" in {
        scalaParser.dataTypeRef(RefinementType) must beSome(
          UnknownTypeRef(sealedFamily1.identifier)
        )
      }
    }
  }
}
