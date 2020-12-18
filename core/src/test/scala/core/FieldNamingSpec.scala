package io.github.scalats.core

import org.specs2.specification.core.Fragments

final class FieldNamingSpec extends org.specs2.mutable.Specification {
  "Field naming" title

  "Identity naming" should {
    import FieldNaming.Identity

    Fragments.foreach(Seq(
      "lorem" -> "lorem",
      "fooBar" -> "fooBar",
      "Ipsum" -> "Ipsum")) {
      case (name, encoded) =>
        s"be ok for '${name}'" in {
          Identity("Foo", name) must_=== encoded
        }
    }
  }

  "Snake case naming" should {
    import FieldNaming.SnakeCase

    Fragments.foreach(Seq(
      "lorem" -> "lorem",
      "fooBar" -> "foo_bar",
      "Ipsum" -> "Ipsum")) {
      case (name, encoded) =>
        s"be ok for '${name}'" in {
          SnakeCase("Bar", name) must_=== encoded
        }
    }
  }
}
