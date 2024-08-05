package io.github.scalats.core

private[core] trait ScalaExtraParserSpec { self: ScalaParserSpec =>
  import ScalaRuntimeFixtures._, results._
  import Internals.ListSet

  "Scala3 support" should {
    "handle opaque type alias" in {
      parseTypes(List(LogOpaqueAliasType -> LogOpaqueAliasTree)) must_=== List(
        logOpaqueAlias.identifier.name -> ListSet(logOpaqueAlias)
      )
    }

    "handle union type" >> {
      "as alias" in {
        parseTypes(
          List(FamilyUnionType -> FamilyUnionTree),
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
        ) must_=== List(
          unionType1.identifier.name -> ListSet(unionType1)
        )
      }

      "as case class field" in {
        parseTypes(List(LoremType -> LoremTree)) must_=== List(
          lorem.identifier.name -> ListSet(lorem)
        )
      }

      "as invariants" in {
        parseTypes(List(IpsumType -> IpsumTree)) must_=== List(
          ipsum.identifier.name -> ListSet(ipsum)
        )
      }
    }

    "handle enum type" >> {
      val colorTpe = ColorType -> ColorTree

      "declaration" in {
        parseTypes(List(colorTpe)) must_=== List(
          color.identifier.name -> ListSet(color)
        )
      }

      "as case class field" in {
        parseTypes(
          List(StyleType -> StyleTree),
          Map(
            fullName(ColorType.typeSymbol) -> ListSet(colorTpe)
          )
        ) must_=== List(style.identifier.name -> ListSet(style))
      }
    }

    "handle from companion object" in {
      val res =
        parseTypes(List(TestClass1CompanionType -> TestClass1CompanionTree))

      res must_=== List(
        caseClass1.identifier.name -> ListSet(
          caseObject1.copy(identifier = caseClass1.identifier)
        )
      )
    }
  }
}
