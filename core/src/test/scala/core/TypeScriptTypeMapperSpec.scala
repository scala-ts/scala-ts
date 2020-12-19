package io.github.scalats.core

import io.github.scalats.typescript._

final class TypeScriptTypeMapperSpec extends org.specs2.mutable.Specification {
  "TypeScript type mapper" title

  import TypeScriptTypeMapper._

  lazy val unresolved: Function3[String, String, TypeRef, String] =
    (_, _, _) => "_tpe_"

  "Mapper" should {
    "map nullable as Option" in {
      nullableAsOption(
        unresolved, "_", "_", NullableType(StringRef)) must beTypedEqualTo(
        Some("Option<_tpe_>"))
    }

    "map number as string" in {
      numberAsString(
        unresolved, "_", "_", NumberRef) must beSome("string")
    }

    "map date as string" in {
      val mapper = dateAsString(unresolved, "_", "_", _: TypeRef)

      mapper(DateRef) must beSome("string") and {
        mapper(DateTimeRef) must beSome("string")
      }
    }

    "be chained" in {
      chain(Seq(numberAsString, dateAsString)).
        aka("chained") must beSome[TypeScriptTypeMapper].which { m =>
          val mapper = m(unresolved, "_", "_", _: TypeRef)

          mapper(NumberRef) must beSome("string") and {
            mapper(DateRef) must beSome("string")
          } and {
            mapper(DateTimeRef) must beSome("string")
          }
        }
    }
  }
}
