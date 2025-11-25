package io.github.scalats.core

import io.github.scalats.ast._

final class TypeMapperSpec extends org.specs2.mutable.Specification {
  "Type mapper".title

  import TypeMapper._

  lazy val unresolved: Function4[Settings, Declaration, Field, TypeRef, String] =
    (_, _, _, _) => "_tpe_"

  lazy val settings = Settings()

  val member = Field("_")

  val owner = InterfaceDeclaration(
    name = "_",
    fields = Internals.ListSet.empty,
    typeParams = List.empty[String],
    superInterface = None,
    union = false
  )

  "Mapper" should {
    "map array" >> {
      "as Array" in {
        arrayAsGeneric(
          unresolved,
          settings,
          owner,
          member,
          ArrayRef(NumberRef.int, false)
        ).aka("TypeScript type") must beSome("Array<_tpe_>")
      }

      "as brackets" in {
        arrayAsBrackets(
          unresolved,
          settings,
          owner,
          member,
          ArrayRef(NumberRef.int, false)
        ).aka("TypeScript type") must beSome("_tpe_[]")
      }
    }

    "map nullable as Option" in {
      nullableAsOption(
        unresolved,
        settings,
        owner,
        member,
        NullableType(StringRef)
      ) must beTypedEqualTo(Some("Option<_tpe_>"))
    }

    "map number as string" in {
      numberAsString(
        unresolved,
        settings,
        owner,
        member,
        NumberRef.int
      ) must beSome(
        "string"
      )
    }

    "map date as string" in {
      val mapper = dateAsString(unresolved, settings, owner, member, _: TypeRef)

      mapper(DateRef) must beSome("string") and {
        mapper(DateTimeRef) must beSome("string")
      }
    }

    "be chained" in {
      chain(Seq(numberAsString, dateAsString))
        .aka("chained") must beSome[TypeMapper].which { m =>
        val mapper = m(unresolved, settings, owner, member, _: TypeRef)

        mapper(NumberRef.int) must beSome("string") and {
          mapper(DateRef) must beSome("string")
        } and {
          mapper(DateTimeRef) must beSome("string")
        }
      }
    }
  }
}
