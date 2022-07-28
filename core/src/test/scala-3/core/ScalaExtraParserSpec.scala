package io.github.scalats.core

private[core] trait ScalaExtraParserSpec { self: ScalaParserSpec =>
  import ScalaRuntimeFixtures._

  "Scala3 support" should {
    "handle opaque type alias" in {
      parseTypes(List(LogOpaqueAliasType -> LogOpaqueAliasTree)) must_=== List(
        logOpaqueAlias
      )
    }

    "handle union type" >> {
      "as alias" in {
        parseTypes(
          List(FamilyUnionType -> FamilyUnionTree),
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
        ) must_=== List(unionType1)
      }

      "as case class field" in {
        parseTypes(List(LoremType -> LoremTree)) must_=== List(lorem)
      }

      "as invariants" in {
        parseTypes(List(IpsumType -> IpsumTree)) must_=== List(ipsum)
      }
    }
  }
}
