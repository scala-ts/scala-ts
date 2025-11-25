package scalats

import io.github.scalats.core.{ Settings, Field, TypeMapper }
import io.github.scalats.ast._

final class CustomTypeMapper extends TypeMapper {

  def apply(
      parent: TypeMapper.Resolved,
      settings: Settings,
      ownerType: Declaration,
      member: Field,
      tpe: TypeRef
    ): Option[String] = {
    val typeNaming = settings.typeNaming(settings, _: TypeRef)

    val tr: TypeRef => String = { ref =>
      apply(parent, settings, ownerType, member, ref).getOrElse(
        parent(settings, ownerType, member, ref)
      )
    }

    val tsType = tpe match {
      case StringRef =>
        "idtlt.string"

      case _: NumberRef =>
        "idtlt.number"

      case BooleanRef =>
        "idtlt.boolean"

      case DateRef | DateTimeRef =>
        "idtlt.isoDate"

      case ArrayRef(innerType, _) =>
        s"idtlt.array(${tr(innerType)})"

      case TupleRef(params) =>
        params.map(tr).mkString("idtlt.tuple(", ", ", ")")

      case custom @ CustomTypeRef(_, Nil) => {
        val n = typeNaming(custom)
        s"ns${n}.idtlt${n}"
      }

      case custom @ CustomTypeRef(_, params) =>
        s"idtlt.unknown /* Unsupported '${typeNaming(custom)}'; Type parameters: ${params.map(tr).mkString(", ")} */"

      case NullableType(innerType) if settings.optionToNullable =>
        s"idtlt.union(${tr(innerType)}, idtlt.null)"

      case NullableType(innerType) =>
        // TODO: ?? member.flags contains Field.omitable
        s"${tr(innerType)}.optional()"
      // TODO: space-monad? string.nullable().map(Option)

      case UnionType(possibilities) =>
        s"idtlt.union(${possibilities.map(tr) mkString ", "})"

      case MapType(keyType, valueType) =>
        s"idtlt.dictionary(${tr(keyType)}, ${tr(valueType)})"

      case _ =>
        s"idtlt.${typeNaming(tpe)}"
    }

    Some(tsType)
  }
}
