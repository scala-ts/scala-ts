package io.github.scalats.python

import io.github.scalats.ast._
import io.github.scalats.core.{ Field, Settings, TypeMapper }

final class PythonTypeMapper extends TypeMapper {

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
        "str"

      case n: NumberRef =>
        n.subtype match {
          case NumberRef.Double =>
            "float"

          case NumberRef.BigDecimal | NumberRef.BigInt =>
            "complex"

          case _ =>
            "int"
        }

      case BooleanRef =>
        "bool"

      case DateRef | DateTimeRef =>
        "datetime.datetime"

      case ThisTypeRef =>
        "self"

      case tpe: SimpleTypeRef => {
        // other simple types (number ...) already handled,
        // so there it's a type parameter
        typeNaming(tpe)
      }

      case ArrayRef(innerType) =>
        s"typing.List[${tr(innerType)}]"

      case SetRef(innerType) =>
        s"typing.Set[${tr(innerType)}]"

      case TupleRef(params) =>
        params.map(tr).mkString("typing.Tuple[", ", ", "]")

      case custom @ CustomTypeRef(_, params @ (_ :: _)) =>
        typeNaming(custom) + params.map(tr).mkString("[", ", ", "]")

      case NullableType(innerType) =>
        s"typing.Optional[${tr(innerType)}]"

      case MapType(keyType, valueType) =>
        s"typing.Dict[${tr(keyType)}, ${tr(valueType)}]"

      case UnionType(possibilities) =>
        possibilities.map(tr).mkString("typing.Union[", ", ", "]")

      case _ =>
        typeNaming(tpe)
    }

    Some(tsType)
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
        s"Optional[${valueType(inner, typeNaming)}]"

      case UnionType(possibilities) =>
        possibilities.map(tr).mkString("typing.Union[", ", ", "]")

      case custom @ (TaggedRef(_, _) | SingletonTypeRef(_, _)) =>
        typeNaming(custom)

      case custom @ CustomTypeRef(_, Nil) =>
        typeNaming(custom)

      case custom @ CustomTypeRef(_, args) => {
        val n = typeNaming(custom)

        s"""${n}${args.map(tr).mkString("[", ", ", "]")}"""
      }

      case ArrayRef(tpe) =>
        s"list[${tr(tpe)}]"

      case MapType(kt, vt) =>
        s"dict[${tr(kt)}, ${tr(vt)}]"

      case _ =>
        typeNaming(vtpe)
    }
  }
}
