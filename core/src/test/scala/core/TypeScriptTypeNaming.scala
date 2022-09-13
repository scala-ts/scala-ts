package io.github.scalats.core

import io.github.scalats.typescript._

final class TypeScriptTypeNamingSpec extends org.specs2.mutable.Specification {
  "TypeScript type naming".title

  val settings = Settings()

  "Default type naming" should {
    val naming = TypeScriptTypeNaming.Identity(settings, _: TypeRef)

    "be applied on simple types" in {
      naming(StringRef) must_=== "string"
    }

    "be applied on Array" in {
      naming(ArrayRef(NumberRef.int)) must_=== "Array"
    }
  }
}
