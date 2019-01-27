package org.scalats.configuration

trait FieldNaming extends (String => String){
  /**
   * Returns the encoded representation for the given field name
   * (e.g. fooBar -> foo_bar if snake case is used).
   */
  def apply(property: String): String
}

object FieldNaming {
  /** 
   * Functional factory
   * 
   * @param naming the naming name
   */
  def apply(
    naming: String,
    convert: String => String
  ): FieldNaming = new Functional(naming, convert)

  /** Identity naming */
  val Identity: FieldNaming = FieldNaming("Identity", identity[String])

  /**
   * For each class property, use the snake case equivalent
   * to name its column (e.g. fooBar -> foo_bar).
   */
  val SnakeCase: FieldNaming = FieldNaming(
    naming = "SnakeCase",
    convert = { property =>
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
    })

  // ---

  private final class Functional(
    private val naming: String,
    f: String => String
  ) extends FieldNaming {
    @inline def apply(property: String): String = f(property)

    @inline override def toString: String = naming

    override def equals(that: Any): Boolean = that match {
      case other: Functional => naming == other.naming
      case _ => false
    }

    override def hashCode: Int = naming.hashCode
  }
}
