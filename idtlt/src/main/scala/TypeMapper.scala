package io.github.scalats.idtlt

import io.github.scalats.core.{
  Settings,
  TypeScriptField,
  TypeScriptTypeMapper
}
import io.github.scalats.typescript._

final class TypeMapper extends TypeScriptTypeMapper {

  def apply(
      parent: TypeScriptTypeMapper.Resolved,
      settings: Settings,
      ownerType: Declaration,
      member: TypeScriptField,
      tpe: TypeRef
    ): Option[String] = ownerType match {
    case SingletonDeclaration(_, _, None) =>
      // IDTLT_TYPE_MAPPER_1
      // Do not generate such singleton as generator
      Some(valueType(tpe, settings.typeNaming(settings, _: TypeRef)))

    case _ => {
      val typeNaming = settings.typeNaming(settings, _: TypeRef)
      val tr: TypeRef => String = { ref =>
        apply(parent, settings, ownerType, member, ref).getOrElse(
          parent(settings, ownerType, member, ref)
        )
      }

      val tsType = tpe match {
        case StringRef =>
          "idtlt.string"

        case NumberRef =>
          "idtlt.number"

        case BooleanRef =>
          "idtlt.boolean"

        case DateRef | DateTimeRef =>
          "idtlt.isoDate"

        case ArrayRef(innerType) =>
          s"idtlt.array(${tr(innerType)})"

        case TupleRef(params) =>
          params.map(tr).mkString("idtlt.tuple(", ", ", ")")

        case tpe: TaggedRef => {
          val n = typeNaming(tpe)
          s"ns${n}.idtlt${n}"
        }

        case custom @ CustomTypeRef(_, Nil) => {
          val n = typeNaming(custom)
          s"ns${n}.idtlt${n}"
        }

        case custom @ CustomTypeRef(_, params) =>
          s"idtlt.unknown /* Unsupported '${typeNaming(custom)}'; Type parameters: ${params.map(tr).mkString(", ")} */"

        case NullableType(innerType) if settings.optionToNullable =>
          s"idtlt.union(${tr(innerType)}, idtlt.null)"

        case NullableType(innerType) =>
          // TODO: ?? member.flags contains TypeScriptField.omitable
          s"${tr(innerType)}.optional()"
        // TODO: space-monad? string.nullable().map(Option)

        case UnionType(possibilities) =>
          s"idtlt.union(${possibilities.map(tr) mkString ", "})"

        case MapType(keyType, valueType) =>
          s"idtlt.dictionary(${tr(keyType)}, ${tr(valueType)}.optional())"

        case _ =>
          s"idtlt.${typeNaming(tpe)}"
      }

      Some(tsType)
    }
  }

  private def valueType(
      vtpe: TypeRef,
      typeNaming: TypeRef => String
    ): String = {
    val tr = valueType(_: TypeRef, typeNaming)

    vtpe match {
      case TupleRef(args) =>
        args.map(tr).mkString("[", ", ", "]")

      case NullableType(inner) =>
        s"undefined | ${valueType(inner, typeNaming)}"

      case UnionType(possibilities) =>
        possibilities.map(tr).mkString(" | ")

      case custom @ (TaggedRef(_, _) | SingletonTypeRef(_, _)) => {
        val n = typeNaming(custom)

        s"ns${n}.${n}"
      }

      case custom @ CustomTypeRef(_, args) => {
        val n = typeNaming(custom)

        s"""ns${n}.${n}${args.map(tr).mkString("<", ", ", ">")}"""
      }

      case ArrayRef(tpe) =>
        s"ReadonlyArray<${tr(tpe)}>"

      case MapType(kt, vt) =>
        s"{ [key: ${tr(kt)}]: ${tr(vt)} }"

      case _ =>
        typeNaming(vtpe)
    }
  }
}
