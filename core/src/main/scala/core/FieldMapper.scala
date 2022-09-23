package io.github.scalats.core

import scala.collection.immutable.Set

import io.github.scalats.ast.{ NullableType, TypeRef }

/**
 * Functional type to customize the field naming and access.
 *
 * {{{
 * import scala.collection.immutable.Set
 * import io.github.scalats.ast.TypeRef
 * import io.github.scalats.core.{
 *   Settings, Field, FieldMapper
 * }
 *
 * class CustomFieldMapper extends FieldMapper {
 *   def apply(
 *     settings: Settings,
 *     ownerType: String,
 *     propertyName: String,
 *     propertyType: TypeRef) =
 *     Field("_" + propertyName, Set.empty)
 * }
 * }}}
 */
trait FieldMapper extends Function4[Settings, String, String, TypeRef, Field] {

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
      propertyType: TypeRef
    ): Field

  override def toString: String = getClass.getName
}

object FieldMapper {

  /** Identity naming */
  object Identity extends FieldMapper {

    def apply(
        settings: Settings,
        ownerType: String,
        propertyName: String,
        propertyType: TypeRef
      ) = Field(propertyName, flags(settings, propertyType))
  }

  /**
   * For each class property, use the snake case equivalent
   * to name its column (e.g. fooBar -> foo_bar).
   */
  object SnakeCase extends FieldMapper {

    def apply(
        settings: Settings,
        ownerType: String,
        propertyName: String,
        propertyType: TypeRef
      ): Field = {
      val length = propertyName.length
      val result = new StringBuilder(length * 2)
      var resultLength = 0
      var wasPrevTranslated = false

      for (i <- 0 until length) {
        var c = propertyName.charAt(i)
        if (i > 0 || i != '_') {
          if (i > 0 && Character.isUpperCase(c)) {
            // append a underscore if the previous result wasn't translated
            if (
              !wasPrevTranslated && resultLength > 0 &&
              result.charAt(resultLength - 1) != '_'
            ) {

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
      Field(result.toString(), flags(settings, propertyType))
    }
  }

  /** Returns the default flags according the type and settings. */
  def flags(settings: Settings, tpe: TypeRef): Set[Field.Flag] =
    tpe match {
      case NullableType(_) if !settings.optionToNullable =>
        Set(Field.omitable)

      case _ =>
        Set.empty[Field.Flag]
    }
}
