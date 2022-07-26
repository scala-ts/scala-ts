package io.github.scalats.core

private[core] trait ScalaExtraParserSpec { self: ScalaParserSpec =>
  import ScalaRuntimeFixtures._

  "Scala3 support" should {
    "handle opaque type alias" in {
      parseTypes(List(LogOpaqueAliasType -> LogOpaqueAliasTree)) must_=== List(
        logOpaqueAlias
      )
    } tag "wip"
  }
}
