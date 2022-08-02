package io.github.scalats.core

import io.github.scalats.core.Internals.ListSet

private[core] trait TranspilerExtraSpec { self: TranspilerSpec =>
  import TranspilerResults.defaultTranspiler

  "Scala3 support" should {
    "transpile enum with extra invariant" in {
      val result = defaultTranspiler(ListSet(ScalaRuntimeFixtures.color))

      result must have size 1 and {
        result must contain(TranspilerExtraSpec.colorDecl)
      }
    }
  }
}

private[core] object TranspilerExtraSpec {
  import io.github.scalats.typescript._
  import TranspilerCompat.ns

  private val colorRef = CustomTypeRef(s"${ns}Color")

  val colorDecl = EnumDeclaration(
    name = s"${ns}Color",
    possibilities = ListSet("Red", "Green", "Blue"),
    values = ListSet(
      ListValue(
        "purple",
        ArrayRef(colorRef),
        colorRef,
        List(
          SelectValue("purple[0]", colorRef, colorRef, "Red"),
          SelectValue("purple[1]", colorRef, colorRef, "Blue")
        )
      )
    )
  )
}
