package io.github.scalats.core

import io.github.scalats.core.Internals.ListSet

private[core] trait TranspilerExtraSpec { self: TranspilerSpec =>
  import TranspilerResults.defaultTranspiler

  "Scala3 support" should {
    import TranspilerExtraSpec.colorDecl

    "transpile enum with extra invariant" in {
      val result = defaultTranspiler(
        Map(
          ScalaRuntimeFixtures.color.identifier.name -> ListSet(
            ScalaRuntimeFixtures.color
          )
        )
      )

      result must_=== List("Color" -> ListSet(colorDecl))
    }
  }
}

private[core] object TranspilerExtraSpec {
  import io.github.scalats.ast._
  import TranspilerCompat.ns

  private val colorTpeRef = CustomTypeRef(s"${ns}Color")
  private val colorObjRef = SingletonTypeRef(s"${ns}Color", ListSet.empty)

  val colorDecl = EnumDeclaration(
    name = s"${ns}Color",
    possibilities = ListSet("Red", "Green", "Blue"),
    values = ListSet(
      ListValue(
        "purple",
        ArrayRef(colorTpeRef, false),
        colorTpeRef,
        List(
          SelectValue("purple[0]", colorTpeRef, colorObjRef, "Red"),
          SelectValue("purple[1]", colorTpeRef, colorObjRef, "Blue")
        )
      )
    )
  )
}
