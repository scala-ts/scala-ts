package scalats

import io.github.scalats.core.{ Settings, Field, TypeMapper }
import io.github.scalats.ast.{ Declaration, DateRef, TypeRef }

final class CustomTypeMapper extends TypeMapper {

  def apply(
      parent: TypeMapper.Resolved,
      settings: Settings,
      ownerType: Declaration,
      member: Field,
      tpe: TypeRef
    ): Option[String] = {
    if (member.name == "_created" && tpe == DateRef) {
      Some("number") // Date as number
    } else {
      None
    }
  }
}
