package io.github.scalats.core

private[core] trait ScalaExtraParserSpec { _: ScalaParserSpec =>
  import ScalaRuntimeFixtures._

  "Scala2 support" should {
    "skip companion object" in {
      val res =
        parseTypes(List(TestClass1CompanionType -> TestClass1CompanionTree))

      res must beEmpty
    }
  }
}
