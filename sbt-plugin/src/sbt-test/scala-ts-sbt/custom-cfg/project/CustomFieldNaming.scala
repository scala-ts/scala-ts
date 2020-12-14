package scalats

class CustomFieldNaming extends io.github.scalats.core.FieldNaming {
  def apply(tpe: String, property: String): String = s"_${property}"
}
