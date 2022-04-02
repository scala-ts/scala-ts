package scalats

import io.github.scalats.core.{
  Settings,
  TypeScriptField,
  TypeScriptTypeMapper
}
import io.github.scalats.typescript.{ Declaration, DateRef, TypeRef }

final class CustomTypeMapper extends TypeScriptTypeMapper {

  def apply(
      parent: TypeScriptTypeMapper.Resolved,
      settings: Settings,
      ownerType: Declaration,
      member: TypeScriptField,
      tpe: TypeRef
    ): Option[String] = {
    if (member.name == "_created" && tpe == DateRef) {
      Some("number") // Date as number
    } else {
      None
    }
  }
}
