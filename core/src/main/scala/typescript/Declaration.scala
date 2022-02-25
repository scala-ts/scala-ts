package io.github.scalats.typescript

import io.github.scalats.core.Internals.ListSet

/** A TypeScript type declaration */
sealed trait Declaration {

  /** The type name */
  def name: String

  def reference: TypeRef = CustomTypeRef(name, List.empty)
}

object Declaration {
  sealed trait Kind

  case object Interface extends Kind

  case object Class extends Kind

  case object Singleton extends Kind

  case object Enum extends Kind

  case object Union extends Kind

  case object Tagged extends Kind

  case object Value extends Kind

  object Kind {

    def unapply(repr: String): Option[Kind] = repr match {
      case "interface" => Some(Interface)
      case "class"     => Some(Class)
      case "singleton" => Some(Singleton)
      case "enum"      => Some(Enum)
      case "union"     => Some(Union)
      case "tagged"    => Some(Tagged)
      case "value"     => Some(Value)
      case _           => None
    }
  }
}

/**
 * A member [[Declaration]] (field/property).
 *
 * @param name the member name
 * @param typeRef the reference for the member type
 */
case class Member(name: String, typeRef: TypeRef)

/**
 * An interface declaration.
 *
 * @param fields the interface fields
 * @param typeParams the type parameters for the interface
 * @param superInterface the super interface (if any)
 * @param union this interface represents a union type
 */
final class InterfaceDeclaration private (
    val name: String,
    val fields: ListSet[Member],
    val typeParams: List[String],
    val superInterface: Option[InterfaceDeclaration],
    val union: Boolean)
    extends Declaration {

  override def reference: TypeRef =
    CustomTypeRef(name, typeParams.map(CustomTypeRef(_)))

  private lazy val tupled =
    Tuple5(name, fields.toList, typeParams, superInterface, union)

  override def toString: String = s"InterfaceDeclaration${tupled.toString}"

  override def hashCode: Int = tupled.hashCode

  override def equals(that: Any): Boolean = that match {
    case other: InterfaceDeclaration =>
      this.tupled == other.tupled

    case _ =>
      false
  }

  @SuppressWarnings(Array("VariableShadowing"))
  def copy(
      name: String = this.name,
      fields: ListSet[Member] = this.fields,
      typeParams: List[String] = this.typeParams,
      superInterface: Option[InterfaceDeclaration] = this.superInterface,
      union: Boolean = this.union
    ): InterfaceDeclaration =
    new InterfaceDeclaration(name, fields, typeParams, superInterface, union)
}

object InterfaceDeclaration {

  def apply(
      name: String,
      fields: ListSet[Member],
      typeParams: List[String],
      superInterface: Option[InterfaceDeclaration],
      union: Boolean
    ): InterfaceDeclaration =
    new InterfaceDeclaration(name, fields, typeParams, superInterface, union)

  def unapply(
      decl: InterfaceDeclaration
    ): Option[Tuple5[String, ListSet[Member], List[String], Option[InterfaceDeclaration], Boolean]] =
    Some(
      Tuple5(
        decl.name,
        decl.fields,
        decl.typeParams,
        decl.superInterface,
        decl.union
      )
    )
}

case class TaggedDeclaration(
    name: String,
    field: Member)
    extends Declaration {
  override def reference: TypeRef = CustomTypeRef(name, List.empty)
}

final class ValueMemberDeclaration private[scalats] (
    val owner: SingletonDeclaration,
    val value: Value)
    extends Declaration {

  @inline def name = value.name
  @inline override def reference = value.reference

  private val tupled = owner -> value

  override def toString: String = s"ValueMemberDeclaration${tupled.toString}"

  override def hashCode: Int = tupled.hashCode

  override def equals(that: Any): Boolean = that match {
    case other: ValueMemberDeclaration =>
      this.tupled == other.tupled

    case _ =>
      false
  }
}

object ValueMemberDeclaration {

  def apply(owner: SingletonDeclaration, value: Value): ValueMemberDeclaration =
    new ValueMemberDeclaration(owner, value)

  def unapply(decl: ValueMemberDeclaration): Option[Value] = Some(decl.value)
}

/**
 * Declaration of the body/rhs for a [[Value]],
 * either as a whole member or part of (e.g. inside [[ListValue]]).
 *
 * @see [[ValueMemberDeclaration]]
 */
final class ValueBodyDeclaration private[scalats] (
    val member: ValueMemberDeclaration,
    val value: Value)
    extends Declaration {

  @inline def name = value.name
  @inline override def reference = value.reference
  @inline def owner = member.owner

  private val tupled = member -> value

  override def toString: String = s"ValueBodyDeclaration${tupled.toString}"

  override def hashCode: Int = tupled.hashCode

  override def equals(that: Any): Boolean = that match {
    case other: ValueBodyDeclaration =>
      this.tupled == other.tupled

    case _ =>
      false
  }
}

object ValueBodyDeclaration {

  def apply(
      member: ValueMemberDeclaration,
      value: Value
    ): ValueBodyDeclaration =
    new ValueBodyDeclaration(member, value)

  def unapply(decl: ValueBodyDeclaration): Option[Value] = Some(decl.value)
}

/**
 * A singleton declaration.
 *
 * @param values the invariant values
 * @param superInterface the super interface (if any)
 */
final class SingletonDeclaration private (
    val name: String,
    val values: ListSet[Value],
    val superInterface: Option[InterfaceDeclaration])
    extends Declaration {
  override def reference: TypeRef = SingletonTypeRef(name, values)

  private lazy val tupled = Tuple3(name, values.toList, superInterface)

  override def toString: String = s"SingletonDeclaration${tupled.toString}"

  override def hashCode: Int = tupled.hashCode

  override def equals(that: Any): Boolean = that match {
    case other: SingletonDeclaration =>
      this.tupled == other.tupled

    case _ =>
      false
  }

  private[scalats] def noSuperInterface: SingletonDeclaration =
    superInterface.fold(this)(_ =>
      new SingletonDeclaration(this.name, this.values, None)
    )
}

object SingletonDeclaration {

  def apply(
      name: String,
      values: ListSet[Value],
      superInterface: Option[InterfaceDeclaration]
    ): SingletonDeclaration =
    new SingletonDeclaration(name, values, superInterface)

  def unapply(
      decl: SingletonDeclaration
    ): Option[(String, ListSet[Value], Option[InterfaceDeclaration])] =
    Some(Tuple3(decl.name, decl.values, decl.superInterface))
}

/**
 * A declaration for an enumerated type.
 *
 * @param values the allowed values
 */
case class EnumDeclaration(
    name: String,
    values: ListSet[String])
    extends Declaration

case class UnionDeclaration(
    name: String,
    fields: ListSet[Member],
    possibilities: ListSet[TypeRef with UnionMemberRef],
    superInterface: Option[InterfaceDeclaration])
    extends Declaration
