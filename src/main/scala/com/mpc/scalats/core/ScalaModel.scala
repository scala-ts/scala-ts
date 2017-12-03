package com.mpc.scalats.core

object ScalaModel {

  sealed trait TypeRef

  case class OptionRef(innerType: TypeRef) extends TypeRef

  case class UnionRef(innerType: TypeRef, innerType2: TypeRef) extends TypeRef

  case class MapRef(keyType: TypeRef, valueType: TypeRef) extends TypeRef

  case class CaseClassRef(name: String, typeArgs: List[TypeRef]) extends TypeRef

  case class SeqRef(innerType: TypeRef) extends TypeRef

  case class Entity(name: String, members: List[EntityMember], params: List[String], baseTypes: List[String], isTrait: Boolean)

  case class EntityMember(name: String, typeRef: TypeRef)

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
