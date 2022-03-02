package scalats

import io.github.scalats.core.{
  Settings,
  TypeScriptFieldMapper,
  TypeScriptField
}

class CustomFieldMapper extends io.github.scalats.core.TypeScriptFieldMapper {

  def apply(
      settings: Settings,
      ownerType: String,
      propertyName: String,
      propertyType: io.github.scalats.typescript.TypeRef
    ) =
    TypeScriptField(s"_${propertyName}", scala.collection.immutable.Set.empty)
}
