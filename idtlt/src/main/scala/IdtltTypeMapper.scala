package io.github.scalats.idtlt

import io.github.scalats.ast._
import io.github.scalats.core.{ Field, Settings, TypeMapper }

final class IdtltTypeMapper extends TypeMapper {

  def apply(
      parent: TypeMapper.Resolved,
      settings: Settings,
      ownerType: Declaration,
      member: Field,
      tpe: TypeRef
    ): Option[String] = ownerType match {
    case SingletonDeclaration(_, _, None) => {
      // IDTLT_TYPE_MAPPER_1
      // Do not generate such singleton as generator

      Some(valueType(tpe, settings.typeNaming(settings, _: TypeRef)))
    }

    case _ => {
      val tr: TypeRef => String = { ref =>
        apply(parent, settings, ownerType, member, ref).getOrElse(
          parent(settings, ownerType, member, ref)
        )
      }

      val typeNaming = settings.typeNaming(settings, _: TypeRef)

      val tsType = tpe match {
        case TimeRef | StringRef =>
          "idtlt.string"

        case _: NumberRef =>
          "idtlt.number"

        case BooleanRef =>
          "idtlt.boolean"

        case DateRef | DateTimeRef =>
          "idtlt.isoDate"

        case ArrayRef(innerType, false) =>
          s"idtlt.readonlyArray(${tr(innerType)})"

        case ArrayRef(innerType, true) =>
          s"(() => { const av = idtlt.readonlyArray(${tr(innerType)}); av.meta.minLength = 1; av.and(([head, ...tail]) => (head !== undefined) ? idtlt.Ok([head, ...tail] as const) : idtlt.Err('Invalid non empty array')) })()"

        case SetRef(innerType) =>
          s"idtlt.arrayAsSet(${tr(innerType)}).map(set => { type extractGeneric<Type> = Type extends Set<infer X> ? X : never; type extracted = extractGeneric<typeof set>; return set as ReadonlySet<extracted> })"

        case TupleRef(innerTypes) =>
          s"idtlt.tuple(${innerTypes.map(tr) mkString ", "})"

        case tpe @ (TaggedRef(_, _) | CustomTypeRef(_, Nil)) => {
          val n = typeNaming(tpe)
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

        case MapType(kt, vt) =>
          s"idtlt.dictionary(${tr(kt)}, ${tr(vt)}.optional())"

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
        s"undefined | ${tr(inner)}"

      case UnionType(possibilities) =>
        possibilities.map(tr).mkString(" | ")

      case custom @ (TaggedRef(_, _) | SingletonTypeRef(_, _) |
          CustomTypeRef(_, Nil)) => {
        val n = typeNaming(custom)

        s"ns${n}.${n}"
      }

      case custom @ CustomTypeRef(_, args) => {
        val n = typeNaming(custom)

        s"""ns${n}.${n}${args.map(tr).mkString("<", ", ", ">")}"""
      }

      case SetRef(innerType) =>
        s"ReadonlySet<${tr(innerType)}>"

      case ArrayRef(tpe, false) =>
        s"ReadonlyArray<${tr(tpe)}>"

      case ArrayRef(tpe, true) => {
        val elmTpe = tr(tpe)
        s"readonly [$elmTpe, ...ReadonlyArray<${elmTpe}>]"
      }

      case MapType(kt, vt) =>
        s"Readonly<Map<${tr(kt)}, ${tr(vt)}>>"

      case tpe: SimpleTypeRef =>
        typeNaming(tpe)

    }
  }
}
