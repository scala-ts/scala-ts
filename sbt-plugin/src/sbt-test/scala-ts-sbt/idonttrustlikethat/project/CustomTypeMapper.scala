package scalats

import io.github.scalats.core.{
  Settings,
  TypeScriptField,
  TypeScriptTypeMapper
}
import io.github.scalats.typescript._

final class CustomTypeMapper extends TypeScriptTypeMapper {
  def apply(
    parent: TypeScriptTypeMapper.Resolved,
    settings: Settings,
    ownerType: String,
    member: TypeScriptField,
    tpe: TypeRef): Option[String] = {
    val typeNaming = settings.typeNaming(settings, _: TypeRef)

    val tr: TypeRef => String = { ref =>
      apply(parent, settings, ownerType, member, ref).
        getOrElse(parent(settings, ownerType, member, ref))
    }

    // TODO: Return string (when no longer partial match)
    tpe match {
      case StringRef =>
        Some("idtlt.string")

      case NumberRef =>
        Some("idtlt.number")

      case BooleanRef =>
        Some("idtlt.boolean")

      case DateRef | DateTimeRef =>
        Some("idtlt.isoDate")

      case ArrayRef(innerType) =>
        Some(s"idtlt.array(${tr(innerType)})")

      case TupleRef(params) =>
        Some(params.map(tr).mkString("idtlt.tuple(", ", ", ")"))

      case custom @ CustomTypeRef(_, Nil) =>
        Some(s"idtlt${typeNaming(custom)}")

        /* TODO:
      case custom @ CustomTypeRef(_, params) =>
        Some(s"idtlt.unknown // Unsupported
        s"${typeNaming(custom)}<${params.map(tr).mkString(", ")}>"
       */

    case _ =>
      None
    }
  }
}
