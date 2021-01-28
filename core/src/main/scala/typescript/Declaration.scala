package io.github.scalats.typescript

import scala.collection.immutable.ListSet

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

  object Kind {
    def unapply(repr: String): Option[Kind] = repr match {
      case "interface" => Some(Interface)
      case "class" => Some(Class)
      case "singleton" => Some(Singleton)
      case "enum" => Some(Enum)
      case "union" => Some(Union)
      case _ => None
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
case class InterfaceDeclaration(
  name: String,
  fields: ListSet[Member],
  typeParams: List[String],
  superInterface: Option[InterfaceDeclaration],
  union: Boolean) extends Declaration {
  override def reference: TypeRef =
    CustomTypeRef(name, typeParams.map(CustomTypeRef(_)))

}

/**
 * A value [[Declaration]].
 *
 * @param name the member name
 * @param typeRef the reference for the member type
 * @param rawValue
 */
case class Value(
  name: String,
  typeRef: TypeRef,
  rawValue: String)

/**
 * A singleton declaration.
 *
 * @param values the invariant values
 * @param superInterface the super interface (if any)
 */
case class SingletonDeclaration(
  name: String,
  values: ListSet[Value],
  superInterface: Option[InterfaceDeclaration]) extends Declaration

/**
 * A declaration for an enumerated type.
 *
 * @param values the allowed values
 */
case class EnumDeclaration(
  name: String,
  values: ListSet[String]) extends Declaration

case class UnionDeclaration(
  name: String,
  fields: ListSet[Member],
  possibilities: ListSet[CustomTypeRef],
  superInterface: Option[InterfaceDeclaration]) extends Declaration
