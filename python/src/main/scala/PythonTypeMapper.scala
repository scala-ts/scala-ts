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

      case TimeRef =>
        "time.struct_time"

      case DateRef | DateTimeRef =>
        "datetime.datetime"

      case ThisTypeRef =>
        "self"

      case tpe: SimpleTypeRef => {
        // other simple types (number ...) already handled,
        // so there it's a type parameter
        typeNaming(tpe)
      }

      case ArrayRef(innerType, _) =>
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
}
