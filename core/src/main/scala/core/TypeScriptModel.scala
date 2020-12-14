package io.github.scalats.core

import scala.collection.immutable.ListSet

object TypeScriptModel {
  sealed trait Declaration

  sealed trait TypeRef

  case class CustomTypeRef(
    name: String,
    typeArgs: List[TypeRef]) extends TypeRef

  case class ArrayRef(innerType: TypeRef) extends TypeRef

  case class InterfaceDeclaration(
    name: String,
    fields: ListSet[Member],
    typeParams: List[String],
    superInterface: Option[InterfaceDeclaration]) extends Declaration
  // TODO: Support mapping of typeParams with superInterface

  case class Member(name: String, typeRef: TypeRef)

  case class ClassDeclaration(
    name: String,
    constructor: ClassConstructor,
    values: ListSet[Member],
    typeParams: List[String],
    superInterface: Option[InterfaceDeclaration]) extends Declaration

  case class SingletonDeclaration(
    name: String,
    values: ListSet[Member],
    superInterface: Option[InterfaceDeclaration]) extends Declaration

  case class EnumDeclaration(
    name: String,
    values: ListSet[String]) extends Declaration

  case class UnionDeclaration(
    name: String,
    fields: ListSet[Member],
    possibilities: ListSet[CustomTypeRef],
    superInterface: Option[InterfaceDeclaration]) extends Declaration

  case class ClassConstructor(parameters: ListSet[ClassConstructorParameter])

  case class ClassConstructorParameter(
    name: String,
    typeRef: TypeRef)

  case class UnknownTypeRef(name: String) extends TypeRef

  case class TupleRef(typeArgs: List[TypeRef]) extends TypeRef

  sealed class SimpleTypeRef private[core] (val name: String) extends TypeRef {
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
    def apply(name: String) = new SimpleTypeRef(name)

    def unapply(ref: SimpleTypeRef): Option[String] = Option(ref.name)
  }

  case object NumberRef extends SimpleTypeRef("number")

  case object StringRef extends SimpleTypeRef("string")

  case object BooleanRef extends SimpleTypeRef("boolean")

  case object DateRef extends SimpleTypeRef("Date")

  case object DateTimeRef extends SimpleTypeRef("DateTime")

  case object NullRef extends SimpleTypeRef("null")

  case object UndefinedRef extends SimpleTypeRef("undefined")

  case class UnionType(possibilities: ListSet[TypeRef]) extends TypeRef

  case class MapType(keyType: TypeRef, valueType: TypeRef) extends TypeRef
}
