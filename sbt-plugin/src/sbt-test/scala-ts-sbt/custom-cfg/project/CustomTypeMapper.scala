package scalats

import io.github.scalats.core.{ TypeScriptField, TypeScriptTypeMapper }
import io.github.scalats.typescript.{ DateRef, TypeRef }

final class CustomTypeMapper extends TypeScriptTypeMapper {
  def apply(
    parent: TypeScriptTypeMapper.Resolved,
    ownerType: String,
    member: TypeScriptField,
    tpe: TypeRef): Option[String] = {
    if (member.name == "_created" && tpe == DateRef) {
      Some("number") // Date as number
    } else {
      None
    }
  }
}
