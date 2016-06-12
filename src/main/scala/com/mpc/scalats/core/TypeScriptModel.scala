package com.mpc.scalats.core

object TypeScriptModel {

  sealed trait TypeRef

  case object NumberRef extends TypeRef

  case object StringRef extends TypeRef

  case class InterfaceRef(name: String) extends TypeRef

  case class ArrayRef(innerType: TypeRef) extends TypeRef

  case object DateRef extends TypeRef

  case object DateTimeRef extends TypeRef

  case class InterfaceDeclaration(name: String, members: List[Member])

  case class Member(name: String, typeRef: TypeRef)

  case class ClassDeclaration(name: String, members: List[Member])

}