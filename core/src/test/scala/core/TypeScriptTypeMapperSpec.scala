package io.github.scalats.core

import io.github.scalats.typescript._

final class TypeScriptTypeMapperSpec extends org.specs2.mutable.Specification {
  "TypeScript type mapper" title

  import TypeScriptTypeMapper._

  lazy val unresolved: Function4[Settings, String, TypeScriptField, TypeRef, String] =
    (_, _, _, _) => "_tpe_"

  lazy val settings = Settings()

  lazy val member = TypeScriptField("_")

  "Mapper" should {
    "map array" >> {
      "as Array" in {
        arrayAsGeneric(unresolved, settings, "_", member, ArrayRef(NumberRef))
          .aka("TypeScript type") must beSome("Array<_tpe_>")
      }

      "as brackets" in {
        arrayAsBrackets(unresolved, settings, "_", member, ArrayRef(NumberRef))
          .aka("TypeScript type") must beSome("_tpe_[]")
      }
    }

    "map nullable as Option" in {
      nullableAsOption(
        unresolved,
        settings,
        "_",
        member,
        NullableType(StringRef)
      ) must beTypedEqualTo(Some("Option<_tpe_>"))
    }

    "map number as string" in {
      numberAsString(unresolved, settings, "_", member, NumberRef) must beSome(
        "string"
      )
    }

    "map date as string" in {
      val mapper = dateAsString(unresolved, settings, "_", member, _: TypeRef)

      mapper(DateRef) must beSome("string") and {
        mapper(DateTimeRef) must beSome("string")
      }
    }

    "be chained" in {
      chain(Seq(numberAsString, dateAsString))
        .aka("chained") must beSome[TypeScriptTypeMapper].which { m =>
        val mapper = m(unresolved, settings, "_", member, _: TypeRef)

        mapper(NumberRef) must beSome("string") and {
          mapper(DateRef) must beSome("string")
        } and {
          mapper(DateTimeRef) must beSome("string")
        }
      }
    }
  }
}
