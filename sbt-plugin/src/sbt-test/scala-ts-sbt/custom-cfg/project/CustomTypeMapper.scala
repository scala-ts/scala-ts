package scalats

import io.github.scalats.core.TypeScriptTypeMapper
import io.github.scalats.typescript.{ DateRef, TypeRef }

final class CustomTypeMapper extends TypeScriptTypeMapper {
  def apply(
    parent: TypeScriptTypeMapper.Resolved,
    ownerType: String,
    member: String,
    tpe: TypeRef): Option[String] = {
    if (member == "created" && tpe == DateRef) {
      Some("number") // Date as number
    } else {
      None
    }
  }
}
