package com.mpc.scalats.core

object ScalaModel {

  sealed trait TypeRef

  case class OptionRef(innerType: TypeRef) extends TypeRef

  case class CaseClassRef(name: String, origin: scala.reflect.runtime.universe.Type) extends TypeRef

  case class SeqRef(innerType: TypeRef) extends TypeRef

  case class CaseClass(name: String, members: List[CaseClassMember])

  case class CaseClassMember(name: String, typeRef: TypeRef)

  case class UnknownTypeRef(name: String, origin: scala.reflect.runtime.universe.Type) extends TypeRef

  case object IntRef extends TypeRef

  case object DoubleRef extends TypeRef

  case object BooleanRef extends TypeRef

  case object StringRef extends TypeRef

  case object DateRef extends TypeRef

  case object DateTimeRef extends TypeRef

}