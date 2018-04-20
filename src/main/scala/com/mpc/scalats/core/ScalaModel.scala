package com.mpc.scalats.core

import scala.collection.immutable.ListSet

// TODO: ValueClass

object ScalaModel {
  sealed trait TypeDef {
    def name: String
  }

  case class CaseClass(
    name: String,
    members: List[CaseClassMember],
    params: List[String]) extends TypeDef

  case class CaseObject(name: String) extends TypeDef

  case class SealedUnion(
    name: String,
    members: ListSet[TypeDef]) extends TypeDef

  // ---

  sealed trait TypeRef

  case class OptionRef(innerType: TypeRef) extends TypeRef

  case class UnionRef(possibilities: ListSet[TypeRef]) extends TypeRef

  case class MapRef(keyType: TypeRef, valueType: TypeRef) extends TypeRef

  case class CaseClassRef(name: String, typeArgs: List[TypeRef]) extends TypeRef

  case class SeqRef(innerType: TypeRef) extends TypeRef

  case class CaseClassMember(name: String, typeRef: TypeRef)

  case class UnknownTypeRef(name: String) extends TypeRef

  case class TypeParamRef(name: String) extends TypeRef

  case object IntRef extends TypeRef

  case object LongRef extends TypeRef

  case object DoubleRef extends TypeRef

  case object BooleanRef extends TypeRef

  case object StringRef extends TypeRef

  case object DateRef extends TypeRef

  case object DateTimeRef extends TypeRef
}
