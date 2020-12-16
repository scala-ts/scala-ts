package scalats

import io.github.scalats.core.{TypeScriptTypeMapper,TypeScriptModel}

final class CustomTypeMapper extends TypeScriptTypeMapper {
  def apply(
    ownerType: String,
    member: String,
    tpe: TypeScriptModel.TypeRef): Option[String] = {
    if (member == "created" && tpe == TypeScriptModel.DateRef) {
      Some("number") // Date as number
    } else {
      None
    }
  }
}
