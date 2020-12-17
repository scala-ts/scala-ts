package io.github.scalats.core

import scala.collection.immutable.ListSet

object ScalaModel {
  case class QualifiedIdentifier(
    name: String,
    enclosingClassNames: List[String])

  sealed trait TypeDef {
    def identifier: QualifiedIdentifier
  }

  case class CaseClass(
    identifier: QualifiedIdentifier,
    fields: ListSet[TypeMember],
    values: ListSet[TypeMember],
    typeArgs: List[String]) extends TypeDef

  case class CaseObject(
    identifier: QualifiedIdentifier,
    values: ListSet[TypeMember]) extends TypeDef

  case class SealedUnion(
    identifier: QualifiedIdentifier,
    fields: ListSet[TypeMember],
    possibilities: ListSet[TypeDef]) extends TypeDef

  case class Enumeration(
    identifier: QualifiedIdentifier,
    values: ListSet[String]) extends TypeDef

  // ---

  sealed trait TypeRef

  case class OptionRef(innerType: TypeRef) extends TypeRef

  case class UnionRef(possibilities: ListSet[TypeRef]) extends TypeRef

  /**
   * @param keyType the type of the `Map` keys
   * @param valueType the type of the `Map` values
   */
  case class MapRef(
    keyType: TypeRef,
    valueType: TypeRef) extends TypeRef

  case class CaseClassRef(
    identifier: QualifiedIdentifier,
    typeArgs: List[TypeRef]) extends TypeRef

  case class SeqRef(innerType: TypeRef) extends TypeRef

  case class TypeMember(name: String, typeRef: TypeRef)

  case class UnknownTypeRef(identifier: QualifiedIdentifier) extends TypeRef

  case class TypeParamRef(name: String) extends TypeRef

  case class EnumerationRef(identifier: QualifiedIdentifier) extends TypeRef

  case class TupleRef(typeArgs: List[TypeRef]) extends TypeRef

  // Non generic/simple types

  case object IntRef extends TypeRef

  case object LongRef extends TypeRef

  case object DoubleRef extends TypeRef

  case object BigDecimalRef extends TypeRef

  case object BigIntegerRef extends TypeRef

  case object BooleanRef extends TypeRef

  case object StringRef extends TypeRef

  case object UuidRef extends TypeRef

  case object DateRef extends TypeRef

  case object DateTimeRef extends TypeRef
}
