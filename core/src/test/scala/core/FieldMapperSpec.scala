package io.github.scalats.core

import scala.collection.immutable.Set

import io.github.scalats.ast.NumberRef

import org.specs2.specification.core.Fragments

final class FieldMapperSpec extends org.specs2.mutable.Specification {
  "Field mapper".title

  lazy val settings = Settings()

  "Identity mapper" should {
    import FieldMapper.Identity

    Fragments.foreach(
      Seq("lorem" -> "lorem", "fooBar" -> "fooBar", "Ipsum" -> "Ipsum")
    ) {
      case (name, encoded) =>
        s"be ok for '${name}'" in {
          Identity(settings, "Foo", name, NumberRef.int).aka(
            "field"
          ) must_=== Field(encoded, Set.empty)
        }
    }
  }

  "Snake case mapper" should {
    import FieldMapper.SnakeCase

    Fragments.foreach(
      Seq("lorem" -> "lorem", "fooBar" -> "foo_bar", "Ipsum" -> "Ipsum")
    ) {
      case (name, encoded) =>
        s"be ok for '${name}'" in {
          SnakeCase(settings, "Bar", name, NumberRef.int).aka(
            "field"
          ) must_=== Field(encoded, Set.empty)
        }
    }
  }
}
