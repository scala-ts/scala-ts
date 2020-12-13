package scalats

class CustomFieldNaming extends org.scalats.core.FieldNaming {
  def apply(tpe: String, property: String): String = s"_${property}"
}
