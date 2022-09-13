package io.github.scalats.core

import scala.collection.immutable.Set

import io.github.scalats.typescript.NumberRef

import org.specs2.specification.core.Fragments

final class TypeScriptFieldMapperSpec extends org.specs2.mutable.Specification {
  "Field mapper".title

  lazy val settings = Settings()

  "Identity mapper" should {
    import TypeScriptFieldMapper.Identity

    Fragments.foreach(
      Seq("lorem" -> "lorem", "fooBar" -> "fooBar", "Ipsum" -> "Ipsum")
    ) {
      case (name, encoded) =>
        s"be ok for '${name}'" in {
          Identity(settings, "Foo", name, NumberRef.int).aka(
            "field"
          ) must_=== TypeScriptField(encoded, Set.empty)
        }
    }
  }

  "Snake case mapper" should {
    import TypeScriptFieldMapper.SnakeCase

    Fragments.foreach(
      Seq("lorem" -> "lorem", "fooBar" -> "foo_bar", "Ipsum" -> "Ipsum")
    ) {
      case (name, encoded) =>
        s"be ok for '${name}'" in {
          SnakeCase(settings, "Bar", name, NumberRef.int).aka(
            "field"
          ) must_=== TypeScriptField(encoded, Set.empty)
        }
    }
  }
}
