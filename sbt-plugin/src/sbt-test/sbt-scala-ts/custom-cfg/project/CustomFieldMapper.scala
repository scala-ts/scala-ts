package scalats

import io.github.scalats.core.{ FieldMapper, Field, Settings }

class CustomFieldMapper extends io.github.scalats.core.FieldMapper {

  def apply(
      settings: Settings,
      ownerType: String,
      propertyName: String,
      propertyType: io.github.scalats.ast.TypeRef
    ) =
    Field(s"_${propertyName}", scala.collection.immutable.Set.empty)
}
