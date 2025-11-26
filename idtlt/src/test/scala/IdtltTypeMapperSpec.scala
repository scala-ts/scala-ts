package io.github.scalats.idtlt

import io.github.scalats.ast.{
  ArrayRef,
  InterfaceDeclaration,
  Member,
  NumberRef,
  StringRef,
  TupleRef,
  TypeRef
}
import io.github.scalats.core.{ Field, Settings, TypeMapper }
import io.github.scalats.core.Internals.ListSet

final class IdtltTypeMapperSpec extends org.specs2.mutable.Specification {
  "Idtlt type mapper".title

  "Type mapper" should {
    "support tuple" in {
      mapType(TupleRef(List(StringRef, NumberRef.int))) must beSome(
        "idtlt.tuple(idtlt.string, idtlt.number)"
      )
    }

    "support non empty array" in {
      mapType(ArrayRef(StringRef, true)) must beSome(
        "(() => { const av = idtlt.readonlyArray(idtlt.string); av.meta.minLength = 1; av.and(([head, ...tail]) => (head !== undefined) ? idtlt.Ok([head, ...tail] as const) : idtlt.Err('Invalid non empty array')) })()"
      )
    }
  }

  // ---

  private val mapType = {
    val idtltTypeMapper = new IdtltTypeMapper
    val field = Field("foo", Set.empty)

    { (tpeRef: TypeRef) =>
      val member = Member("foo", tpeRef)
      val decl =
        InterfaceDeclaration("Foo", ListSet(member), List.empty, None, false)

      idtltTypeMapper(
        null.asInstanceOf[TypeMapper.Resolved],
        Settings(),
        decl,
        field,
        tpeRef
      )
    }
  }
}
