package io.github.scalats.typescript

import scala.collection.immutable.{ ListSet, Set }

/** Reference to a builtin type or one declared elsewhere. */
sealed trait TypeRef {
  /* See `Declaration.requires` */
  private[typescript] def requires: Set[TypeRef]
}

private[typescript] sealed trait GenericTypeRef { ref: TypeRef =>
  /** The type name */
  def name: String

  /** The applied type arguments (respect order and duplicate) */
  def typeArgs: List[TypeRef]

  override lazy val toString: String = {
    if (typeArgs.isEmpty) name
    else s"""${name}${typeArgs.mkString("<", ", ", ">")}"""
  }

  private[typescript] def requires: Set[TypeRef] =
    typeArgs.toSet.flatMap { ta: TypeRef => ta.requires }

}

/**
 * Reference to a custom type.
 *
 * @param name the type name
 * @param typeArgs the type arguments (e.g. `string` for `CustomType<string>`)
 */
case class CustomTypeRef(
  name: String,
  typeArgs: List[TypeRef]) extends TypeRef with GenericTypeRef {
  override private[typescript] def requires: Set[TypeRef] =
    super.requires + this
}

/**
 * Reference to an type of `Array` (e.g. `Array<string>`).
 *
 * @param innerType the element type (e.g. `string` for `Array<string>`)
 */
case class ArrayRef(innerType: TypeRef) extends TypeRef {
  @inline private[typescript] def requires = innerType.requires

  override def toString = s"Array<${innerType.toString}>"
}

/**
 * Type which kind is unknown/unsupported.
 */
case class UnknownTypeRef(name: String) extends TypeRef {
  private[typescript] def requires = Set.empty[TypeRef]

  override def toString = s"unknown<$name>"
}

/**
 * Reference to a type of tuple (e.g. `[string, int]`).
 *
 * @param typeArgs the types for the tuple elements
 */
case class TupleRef(typeArgs: List[TypeRef])
  extends TypeRef with GenericTypeRef {

  @SuppressWarnings(Array("ListSize"))
  def name = s"Tuple${typeArgs.size}"
}

private[scalats] sealed class SimpleTypeRef(val name: String) extends TypeRef {
  @inline private[typescript] def requires = Set.empty[TypeRef]

  @inline override def toString = name

  override def hashCode: Int = name.hashCode

  override def equals(that: Any): Boolean = that match {
    case other: SimpleTypeRef =>
      this.name == other.name

    case _ =>
      false
  }
}

object SimpleTypeRef {
  private[scalats] def apply(name: String) = new SimpleTypeRef(name)

  def unapply(ref: SimpleTypeRef): Option[String] = Option(ref.name)
}

case object NumberRef extends SimpleTypeRef("number")

case object StringRef extends SimpleTypeRef("string")

case object BooleanRef extends SimpleTypeRef("boolean")

case object DateRef extends SimpleTypeRef("Date")

case object DateTimeRef extends SimpleTypeRef("DateTime")

/**
 * Reference to a nullable type (e.g. an optional string).
 *
 * @param innerType the inner type (e.g. `string` for nullable string)
 */
case class NullableType(innerType: TypeRef) extends TypeRef {
  @inline private[typescript] def requires = innerType.requires

  override def toString = s"Nullable<${innerType.toString}>"
}

/**
 * Reference to a union type (e.g. `string | number`)
 */
case class UnionType(possibilities: ListSet[TypeRef]) extends TypeRef {
  @inline private[typescript] def requires =
    possibilities.flatMap(_.requires)

  override def toString: String =
    io.github.scalats.core.Internals.list(possibilities).mkString(" | ")
}

/**
 * Reference to a map/dictionary type.
 *
 * @param keyType the type of the keys
 * @param valueType the type of the values
 */
case class MapType(keyType: TypeRef, valueType: TypeRef) extends TypeRef {
  private[typescript] def requires: Set[TypeRef] =
    keyType.requires ++ valueType.requires

  override def toString = s"Map<${keyType}, ${valueType}>"
}
