package io.github.scalats.core

import scala.collection.immutable.ListSet

object TypeScriptModel {
  sealed trait Declaration

  sealed trait TypeRef

  case class CustomTypeRef(
    name: String,
    typeArgs: ListSet[TypeRef]) extends TypeRef

  case class ArrayRef(innerType: TypeRef) extends TypeRef

  case class InterfaceDeclaration(name: String, fields: ListSet[Member], typeParams: ListSet[String], superInterface: Option[InterfaceDeclaration]) extends Declaration
  // TODO: Support mapping of typeParams with superInterface

  case class Member(name: String, typeRef: TypeRef)

  case class ClassDeclaration(
    name: String,
    constructor: ClassConstructor,
    values: ListSet[Member],
    typeParams: ListSet[String],
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

  case object NumberRef extends TypeRef {
    override def toString = "number"
  }

  case object StringRef extends TypeRef {
    override def toString = "string"
  }

  case object BooleanRef extends TypeRef {
    override def toString = "boolean"
  }

  case object DateRef extends TypeRef {
    override def toString = "Date"
  }

  case object DateTimeRef extends TypeRef {
    override def toString = "DateTime"
  }

  case object NullRef extends TypeRef {
    override def toString = "null"
  }

  case object UndefinedRef extends TypeRef {
    override def toString = "undefined"
  }

  case class SimpleTypeRef(name: String) extends TypeRef {
    override def toString = name
  }

  case class UnionType(possibilities: ListSet[TypeRef]) extends TypeRef

  case class MapType(keyType: TypeRef, valueType: TypeRef) extends TypeRef
}
