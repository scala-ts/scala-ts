package com.mpc.scalats.core

import scala.collection.immutable.ListSet

// TODO: ValueClass

object ScalaModel {
  case class TypeName(
    name: String,
    enclosingClassNames: List[String]
  )

  sealed trait TypeDef {
    def name: TypeName
  }

  case class CaseClass(
    name: TypeName,
    fields: ListSet[TypeMember],
    values: ListSet[TypeMember],
    typeArgs: ListSet[String]) extends TypeDef

  case class CaseObject(
    name: TypeName,
    values: ListSet[TypeMember]
  ) extends TypeDef

  case class SealedUnion(
    name: TypeName,
    fields: ListSet[TypeMember],
    possibilities: ListSet[TypeDef]) extends TypeDef

  case class Enumeration(
    name: TypeName,
    values: ListSet[String]) extends TypeDef

  // ---

  sealed trait TypeRef

  case class OptionRef(innerType: TypeRef) extends TypeRef

  case class UnionRef(possibilities: ListSet[TypeRef]) extends TypeRef

  case class MapRef(keyType: TypeRef, valueType: TypeRef) extends TypeRef

  case class CaseClassRef(name: TypeName, typeArgs: ListSet[TypeRef]) extends TypeRef

  case class SeqRef(innerType: TypeRef) extends TypeRef

  case class TypeMember(name: String, typeRef: TypeRef)

  case class UnknownTypeRef(name: TypeName) extends TypeRef

  case class TypeParamRef(name: String) extends TypeRef

  case class EnumerationRef(name: TypeName) extends TypeRef

  case object IntRef extends TypeRef

  case object LongRef extends TypeRef

  case object DoubleRef extends TypeRef

  case object BigDecimalRef extends TypeRef

  case object BooleanRef extends TypeRef

  case object StringRef extends TypeRef

  case object UuidRef extends TypeRef

  case object DateRef extends TypeRef

  case object DateTimeRef extends TypeRef
}
