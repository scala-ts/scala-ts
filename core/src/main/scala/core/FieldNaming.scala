package io.github.scalats.core

// TODO: (String, String) => String with Scalatype as first arg
trait FieldNaming extends Function2[String, String, String] {
  /**
   * Returns the encoded representation for the given field name
   * (e.g. fooBar -> foo_bar if snake case is used).
   *
   * @param tpe the name of Scala class/interface for which the property is defined
   * @param property the property name
   */
  def apply(tpe: String, property: String): String
}

object FieldNaming {
  /** Identity naming */
  object Identity extends FieldNaming {
    def apply(tpe: String, property: String) = property
  }

  /**
   * For each class property, use the snake case equivalent
   * to name its column (e.g. fooBar -> foo_bar).
   */
  object SnakeCase extends FieldNaming {
    def apply(tpe: String, property: String): String = {
      val length = property.length
      val result = new StringBuilder(length * 2)
      var resultLength = 0
      var wasPrevTranslated = false

      for (i <- 0 until length) {
        var c = property.charAt(i)
        if (i > 0 || i != '_') {
          if (i > 0 && Character.isUpperCase(c)) {
            // append a underscore if the previous result wasn't translated
            if (!wasPrevTranslated && resultLength > 0 &&
              result.charAt(resultLength - 1) != '_') {

              result.append('_')
              resultLength += 1
            }
            c = Character.toLowerCase(c)
            wasPrevTranslated = true
          } else {
            wasPrevTranslated = false
          }

          result.append(c)

          resultLength += 1
        }
      }

      // builds the final string
      result.toString()
    }
  }
}
