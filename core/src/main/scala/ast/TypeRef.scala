package io.github.scalats.ast

import io.github.scalats.core.Internals

import Internals.ListSet

/** Reference to a builtin type or one declared elsewhere. */
sealed trait TypeRef {
  /* See `Declaration.requires` */
  private[scalats] def requires: ListSet[TypeRef]

  /** The type name */
  def name: String
}

private[ast] sealed trait GenericTypeRef { ref: TypeRef =>

  /** The type name */
  def name: String

  /** The applied type arguments (respect order and duplicate) */
  def typeArgs: List[TypeRef]

  override lazy val toString: String = {
    if (typeArgs.isEmpty) name
    else s"""${name}${typeArgs.mkString("<", ", ", ">")}"""
  }

  def requires: ListSet[TypeRef] =
    ListSet.empty ++ typeArgs.flatMap { (ta: TypeRef) => ta.requires }

}

sealed trait UnionMemberRef { ref: TypeRef => }

/**
 * Tagged type.
 *
 * @param name the type name
 */
case class TaggedRef(
    name: String,
    tagged: TypeRef)
    extends TypeRef {

  override def requires: ListSet[TypeRef] = tagged.requires + this

  override lazy val toString = s"${name} /* ${tagged.toString} */"
}

/**
 * Reference to a custom type.
 *
 * @param name the type name
 * @param typeArgs the type arguments (e.g. `string` for `CustomType<string>`)
 */
case class CustomTypeRef(
    name: String,
    typeArgs: List[TypeRef] = Nil)
    extends TypeRef
    with UnionMemberRef
    with GenericTypeRef {

  override def requires: ListSet[TypeRef] =
    super.requires + this
}

/**
 * @param name the type name
 * @param values the invariant values
 */
case class SingletonTypeRef(
    name: String,
    values: ListSet[Value])
    extends TypeRef
    with UnionMemberRef {
  override val requires: ListSet[TypeRef] = ListSet(this)

  override lazy val toString = s"#${name}{${values mkString ", "}}"
}

/**
 * Reference to an type of `Array` (e.g. `Array<string>`).
 *
 * @param innerType the element type (e.g. `string` for `Array<string>`)
 */
case class ArrayRef(innerType: TypeRef, nonEmpty: Boolean) extends TypeRef {
  @inline def requires = innerType.requires

  lazy val name = "Array"

  override def toString = s"Array<${innerType.toString}>"
}

/**
 * Reference to an type of `Set` (e.g. `Set<string>`).
 *
 * @param innerType the element type (e.g. `string` for `Set<string>`)
 */
case class SetRef(innerType: TypeRef) extends TypeRef {
  @inline def requires = innerType.requires

  lazy val name = "Set"

  override def toString = s"Set<${innerType.toString}>"
}

/**
 * Reference to a type of tuple (e.g. `[string, int]`).
 *
 * @param typeArgs the types for the tuple elements
 */
case class TupleRef(typeArgs: List[TypeRef])
    extends TypeRef
    with GenericTypeRef {

  @SuppressWarnings(Array("ListSize"))
  def name = s"Tuple${typeArgs.size}"
}

private[scalats] sealed class SimpleTypeRef(val name: String) extends TypeRef {
  @inline def requires = ListSet.empty[TypeRef]

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

final class NumberRef private[scalats] (
    val subtype: NumberRef.Subtype)
    extends SimpleTypeRef("number")

object NumberRef {
  val int = new NumberRef(Int)

  val long = new NumberRef(Long)

  val double = new NumberRef(Double)

  val bigInt = new NumberRef(BigInt)

  val bigDecimal = new NumberRef(BigDecimal)

  // ---

  sealed trait Subtype
  case object BigInt extends Subtype
  case object BigDecimal extends Subtype

  case object Int extends Subtype
  case object Long extends Subtype
  case object Double extends Subtype
}

case object StringRef extends SimpleTypeRef("string")

case object BooleanRef extends SimpleTypeRef("boolean")

case object DateRef extends SimpleTypeRef("Date")

case object DateTimeRef extends SimpleTypeRef("DateTime")

case object TimeRef extends SimpleTypeRef("Time")

private[scalats] case object ThisTypeRef extends SimpleTypeRef("this")

/**
 * Reference to a nullable type (e.g. an optional string).
 *
 * @param innerType the inner type (e.g. `string` for nullable string)
 */
case class NullableType(innerType: TypeRef) extends TypeRef {
  @inline def requires = innerType.requires

  lazy val name = s"${innerType.name} | undefined"

  override def toString = s"Nullable<${innerType.toString}>"
}

/**
 * Reference to a union type (e.g. `string | number`)
 */
case class UnionType(possibilities: ListSet[TypeRef]) extends TypeRef {
  @inline def requires = ListSet.empty[TypeRef]

  lazy val name = possibilities.map(_.name).mkString(" | ")

  override def toString: String =
    possibilities.mkString(" | ")
}

/**
 * Reference to a map/dictionary type.
 *
 * @param keyType the type of the keys
 * @param valueType the type of the values
 */
case class MapType(keyType: TypeRef, valueType: TypeRef) extends TypeRef {

  def requires: ListSet[TypeRef] =
    keyType.requires ++ valueType.requires

  lazy val name = s"{ [key: ${keyType.name}]: ${valueType.name} }"

  override def toString = s"Map<${keyType}, ${valueType}>"
}
