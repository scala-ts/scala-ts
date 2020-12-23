package io.github.scalats.core

import scala.collection.immutable.Set

import io.github.scalats.typescript.TypeRef

/**
 * Functional type to customize the field naming and access.
 *
 * {{{
 * import scala.collection.immutable.Set
 * import io.github.scalats.typescript.TypeRef
 * import io.github.scalats.core.{
 *   Settings, TypeScriptField, TypeScriptFieldMapper
 * }
 *
 * class CustomTypeScriptFieldMapper extends TypeScriptFieldMapper {
 *   def apply(
 *     settings: Settings,
 *     ownerType: String,
 *     propertyName: String,
 *     propertyType: TypeRef) =
 *     TypeScriptField("_" + propertyName, Set.empty)
 * }
 * }}}
 */
trait TypeScriptFieldMapper
  extends Function4[Settings, String, String, TypeRef, TypeScriptField] {

  /**
   * Returns the TypeScript field/signature for the given field name
   * (e.g. `fooBar -> foo_bar` if snake case is used).
   *
   * @param ownerType the name of Scala class/interface for which the property is defined
   * @param propertyName the property name
   * @param propertyType the type transpiled for the specified property
   */
  def apply(
    settings: Settings,
    ownerType: String,
    propertyName: String,
    propertyType: TypeRef): TypeScriptField
  // TODO: Si type Nullable et config.optionToUndefined, positionné omitable:true

}

object TypeScriptFieldMapper {
  /** Identity naming */
  object Identity extends TypeScriptFieldMapper {
    def apply(
      settings: Settings,
      ownerType: String,
      propertyName: String,
      propertyType: TypeRef) = TypeScriptField(propertyName, Set.empty)
  }

  /**
   * For each class property, use the snake case equivalent
   * to name its column (e.g. fooBar -> foo_bar).
   */
  object SnakeCase extends TypeScriptFieldMapper {
    def apply(
      settings: Settings,
      ownerType: String,
      propertyName: String,
      propertyType: TypeRef): TypeScriptField = {
      val length = propertyName.length
      val result = new StringBuilder(length * 2)
      var resultLength = 0
      var wasPrevTranslated = false

      for (i <- 0 until length) {
        var c = propertyName.charAt(i)
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
      TypeScriptField(result.toString(), Set.empty)
    }
  }
}
