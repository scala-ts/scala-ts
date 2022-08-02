package io.github.scalats.core

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
          ) -> (TestClass3Type -> TestClass3Tree),
          fullName(
            TestClass5Type.typeSymbol // 'age' in 6
          ) -> (TestClass5Type -> TestClass5Tree)
        )
      )

      res must contain(caseClass1) and {
        res must contain(caseClass2)
      } and {
        res must contain(caseClass3)
      } and {
        res must contain(caseClass4)
      } and {
        res must contain(caseClass6)
      } and {
        res must contain(caseClass5) // as required for a caseClass6 property
      } and {
        res must have size 6
      }
    }

    "handle either types" in {
      val res = parseTypes(
        List(TestClass7Type -> TestClass7Tree),
        Map(
          fullName(
            TestClass1Type.typeSymbol // 'name' in 7
          ) -> (TestClass1Type -> TestClass1Tree),
          fullName(
            TestClass1BType.typeSymbol // 'name' in 7
          ) -> (TestClass1BType -> TestClass1BTree)
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
      "declaration" in eventually {
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
      }
    }

    "handle sealed trait as union" in {
      val res = parseTypes(
        List(FamilyType -> FamilyTree),
        Map(
          fullName(
            FamilyMember1Type.typeSymbol
          ) -> (FamilyMember1Type -> FamilyMember1Tree),
          fullName(
            FamilyMember2Type.typeSymbol
          ) -> (FamilyMember2Type -> FamilyMember2Tree),
          fullName(
            FamilyMember3Type.typeSymbol
          ) -> (FamilyMember3Type -> FamilyMember3Tree)
        )
      )

      res must contain(sealedFamily1) and {
        res must have size 1
      }
    }
  }
}
