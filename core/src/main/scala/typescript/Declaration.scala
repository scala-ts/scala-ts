package io.github.scalats.typescript

import scala.collection.immutable.{ ListSet, Set }

/** A TypeScript type declaration */
sealed trait Declaration {
  /** The type name */
  def name: String

  private[scalats] def requires: Set[TypeRef]
}

object Declaration {
  sealed trait Kind

  case object Interface extends Kind

  case object Class extends Kind

  case object Singleton extends Kind

  case object Enum extends Kind

  case object Union extends Kind
}

/**
 * A [[Declaration]] member (field/property).
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
 */
case class InterfaceDeclaration(
  name: String,
  fields: ListSet[Member],
  typeParams: List[String],
  superInterface: Option[InterfaceDeclaration]) extends Declaration {

  private[scalats] def requires: Set[TypeRef] =
    (fields.flatMap(_.typeRef.requires).filterNot {
      case TypeRef.Named(`name`) => true /* skip self reference */
      case _ => false
    }) ++ superInterface.toSet.map { si: InterfaceDeclaration =>
      CustomTypeRef(si.name, List.empty)
    }
}

/* TODO: (medium priority) Remove
 * A class declaration.
 *
 * @param constructor the class constructor
 * @param values the class invariants values
 * @param typeParams the type parameters for the interface
 * @param superInterface the super interface (if any)
case class ClassDeclaration(
  name: String,
  constructor: ClassConstructor,
  values: ListSet[Member],
  typeParams: List[String],
  superInterface: Option[InterfaceDeclaration]) extends Declaration {
  private[scalats] def requires: Set[TypeRef] =
    (constructor.parameters.flatMap(_.typeRef.requires).filterNot {
      case TypeRef.Named(`name`) => true /* skip self reference */
      case _ => false
    }) ++ (values.flatMap(_.typeRef.requires).filterNot {
      case TypeRef.Named(`name`) => true /* skip self reference */
      case _ => false
    }) ++ superInterface.toSet.map { si: InterfaceDeclaration =>
      CustomTypeRef(si.name, List.empty)
    }
}

 * A class constructor.
 *
 * @param parameters the parameter for the class constructor
case class ClassConstructor(parameters: ListSet[ClassConstructorParameter])

 * A parameter for a class constructor.
 *
 * @param name the parameter name
 * @param typeRef the reference to the parameter type
case class ClassConstructorParameter(
  name: String,
  typeRef: TypeRef)
 */

/**
 * A singleton declaration.
 *
 * @param values the invariant values
 * @param superInterface the super interface (if any)
 */
case class SingletonDeclaration(
  name: String,
  values: ListSet[Member],
  superInterface: Option[InterfaceDeclaration]) extends Declaration {
  private[scalats] def requires: Set[TypeRef] =
    (values.flatMap(_.typeRef.requires).filterNot {
      case TypeRef.Named(`name`) => true /* skip self reference */
      case _ => false
    }) ++ superInterface.toSet.map { si: InterfaceDeclaration =>
      CustomTypeRef(si.name, List.empty)
    }
}

/**
 * A declaration for an enumerated type.
 *
 * @param values the allowed values
 */
case class EnumDeclaration(
  name: String,
  values: ListSet[String]) extends Declaration {
  @inline private[scalats] def requires = Set.empty[TypeRef]
}

case class UnionDeclaration(
  name: String,
  fields: ListSet[Member],
  possibilities: ListSet[CustomTypeRef],
  superInterface: Option[InterfaceDeclaration]) extends Declaration {
  private[scalats] def requires: Set[TypeRef] =
    (fields.flatMap(_.typeRef.requires).filterNot {
      case TypeRef.Named(`name`) => true /* skip self reference */
      case _ => false
    }) ++ superInterface.toSet.map { si: InterfaceDeclaration =>
      CustomTypeRef(si.name, List.empty)
    }
}
