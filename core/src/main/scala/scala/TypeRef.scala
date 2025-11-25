package io.github.scalats.scala

import io.github.scalats.core.Internals.ListSet

sealed trait TypeRef

case class OptionRef(innerType: TypeRef) extends TypeRef

case class UnionRef(possibilities: ListSet[TypeRef]) extends TypeRef

/**
 * @param keyType the type of the `Map` keys
 * @param valueType the type of the `Map` values
 */
case class MapRef(
    keyType: TypeRef,
    valueType: TypeRef)
    extends TypeRef

case class CaseClassRef(
    identifier: QualifiedIdentifier,
    typeArgs: List[TypeRef])
    extends TypeRef

case class CaseObjectRef(identifier: QualifiedIdentifier) extends TypeRef

case class ListRef(innerType: TypeRef, nonEmpty: Boolean) extends TypeRef

case class SetRef(innerType: TypeRef) extends TypeRef

case class UnknownTypeRef(identifier: QualifiedIdentifier) extends TypeRef

case class TypeParamRef(name: String) extends TypeRef

case class EnumerationRef(identifier: QualifiedIdentifier) extends TypeRef

case class TaggedRef(
    identifier: QualifiedIdentifier,
    tagged: TypeRef)
    extends TypeRef

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

case object TimeRef extends TypeRef

private[scalats] case object ThisTypeRef extends TypeRef

// TODO: Regex
