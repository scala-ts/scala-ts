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
    values: ListSet[TypeInvariant],
    typeArgs: List[String]) extends TypeDef

  case class ValueClass(
    identifier: QualifiedIdentifier,
    field: TypeMember) extends TypeDef

  case class CaseObject(
    identifier: QualifiedIdentifier,
    values: ListSet[TypeInvariant]) extends TypeDef

  case class SealedUnion(
    identifier: QualifiedIdentifier,
    fields: ListSet[TypeMember],
    possibilities: ListSet[TypeDef]) extends TypeDef

  case class EnumerationDef(
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

  case class CollectionRef(innerType: TypeRef) extends TypeRef

  sealed class TypeMember(
    val name: String,
    val typeRef: TypeRef) {
    private lazy val tupled = name -> typeRef

    override def toString = s"TypeMember${tupled.toString}"

    override def hashCode: Int = tupled.hashCode

    override def equals(that: Any): Boolean = that match {
      case other: TypeMember =>
        this.tupled == other.tupled

      case _ =>
        false
    }
  }

  object TypeMember {
    @inline def apply(name: String, typeRef: TypeRef): TypeMember =
      new TypeMember(name, typeRef)
  }

  final class TypeInvariant(
    name: String,
    typeRef: TypeRef,
    val value: String) extends TypeMember(name, typeRef) {

    private lazy val tupled = Tuple3(name, typeRef, value)

    override def toString = s"TypeInvariant${tupled.toString}"

    override def hashCode: Int = tupled.hashCode

    override def equals(that: Any): Boolean = that match {
      case other: TypeInvariant =>
        this.tupled == other.tupled

      case _ =>
        false
    }
  }

  object TypeInvariant {
    @inline def apply(
      name: String,
      typeRef: TypeRef,
      value: String): TypeInvariant = new TypeInvariant(name, typeRef, value)
  }

  case class UnknownTypeRef(identifier: QualifiedIdentifier) extends TypeRef

  case class TypeParamRef(name: String) extends TypeRef

  case class EnumerationRef(identifier: QualifiedIdentifier) extends TypeRef

  case class TaggedRef(
    identifier: QualifiedIdentifier,
    tagged: TypeRef) extends TypeRef

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
