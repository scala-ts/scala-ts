package io.github.scalats.scala

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

sealed class TypeInvariant protected (
    name: String,
    typeRef: TypeRef)
    extends TypeMember(name, typeRef)

sealed trait SimpleInvariant { _self: TypeInvariant => }

object TypeInvariant {
  private[scalats] type Simple = TypeInvariant with SimpleInvariant
}

final class LiteralInvariant(
    name: String,
    typeRef: TypeRef,
    val value: String)
    extends TypeInvariant(name, typeRef)
    with SimpleInvariant {

  private lazy val tupled = Tuple3(name, typeRef, value)

  override def toString = s"LiteralInvariant${tupled.toString}"

  override def hashCode: Int = tupled.hashCode

  override def equals(that: Any): Boolean = that match {
    case other: LiteralInvariant =>
      this.tupled == other.tupled

    case _ =>
      false
  }
}

object LiteralInvariant {

  @inline def apply(
      name: String,
      typeRef: TypeRef,
      value: String
    ): LiteralInvariant = new LiteralInvariant(name, typeRef, value)
}

final class SelectInvariant(
    name: String,
    typeRef: TypeRef,
    val qualifier: TypeRef,
    val term: String)
    extends TypeInvariant(name, typeRef)
    with SimpleInvariant {

  private lazy val tupled = Tuple4(name, typeRef, qualifier, term)

  override def toString = s"SelectInvariant${tupled.toString}"

  override def hashCode: Int = tupled.hashCode

  override def equals(that: Any): Boolean = that match {
    case other: SelectInvariant =>
      this.tupled == other.tupled

    case _ =>
      false
  }
}

object SelectInvariant {

  @inline def apply(
      name: String,
      typeRef: TypeRef,
      qualifier: TypeRef,
      term: String
    ): SelectInvariant = new SelectInvariant(name, typeRef, qualifier, term)
}

/**
 * @param valueTypeRef the elements type
 */
final class TupleInvariant(
    name: String,
    typeRef: TypeRef, // TODO: Refactor out of ctor
    val values: List[TypeInvariant])
    extends TypeInvariant(name, typeRef) {

  private lazy val tupled = Tuple3(name, typeRef, values)

  override def toString = s"TupleInvariant${tupled.toString}"

  override def hashCode: Int = tupled.hashCode

  override def equals(that: Any): Boolean = that match {
    case other: TupleInvariant =>
      this.tupled == other.tupled

    case _ =>
      false
  }
}

object TupleInvariant {

  @inline def apply(
      name: String,
      typeRef: TypeRef,
      values: List[TypeInvariant]
    ): TupleInvariant = new TupleInvariant(name, typeRef, values)
}

/**
 * @param valueTypeRef the elements type
 */
final class ListInvariant(
    name: String,
    typeRef: TypeRef, // TODO: Refactor out of ctor
    val valueTypeRef: TypeRef,
    val values: List[TypeInvariant])
    extends TypeInvariant(name, typeRef) {

  private lazy val tupled = Tuple4(name, typeRef, valueTypeRef, values)

  override def toString = s"ListInvariant${tupled.toString}"

  override def hashCode: Int = tupled.hashCode

  override def equals(that: Any): Boolean = that match {
    case other: ListInvariant =>
      this.tupled == other.tupled

    case _ =>
      false
  }
}

object ListInvariant {

  @inline def apply(
      name: String,
      typeRef: TypeRef,
      valueTypeRef: TypeRef,
      values: List[TypeInvariant]
    ): ListInvariant = new ListInvariant(name, typeRef, valueTypeRef, values)
}

/**
 * @param valueTypeRef the elements type (inside the list)
 */
final class MergedListsInvariant(
    name: String,
    val valueTypeRef: TypeRef,
    val children: List[TypeInvariant])
    extends TypeInvariant(name, ListRef(valueTypeRef)) {

  private lazy val tupled = Tuple3(name, valueTypeRef, children)

  override def toString = s"MergedListsInvariant${tupled.toString}"

  override def hashCode: Int = tupled.hashCode

  override def equals(that: Any): Boolean = that match {
    case other: MergedListsInvariant =>
      this.tupled == other.tupled

    case _ =>
      false
  }
}

object MergedListsInvariant {

  @inline def apply(
      name: String,
      valueTypeRef: TypeRef,
      children: List[TypeInvariant]
    ): MergedListsInvariant =
    new MergedListsInvariant(name, valueTypeRef, children)

  def unapply(
      inv: MergedListsInvariant
    ): Option[(String, TypeRef, List[TypeInvariant])] = Some(inv.tupled)
}

/**
 * @param valueTypeRef the elements type
 */
final class SetInvariant(
    name: String,
    typeRef: TypeRef,
    val valueTypeRef: TypeRef,
    val values: Set[TypeInvariant])
    extends TypeInvariant(name, typeRef) {
  private lazy val tupled = Tuple4(name, typeRef, valueTypeRef, values)

  override def toString = s"SetInvariant${tupled.toString}"

  override def hashCode: Int = tupled.hashCode

  override def equals(that: Any): Boolean = that match {
    case other: SetInvariant =>
      this.tupled == other.tupled

    case _ =>
      false
  }
}

object SetInvariant {

  @inline def apply(
      name: String,
      typeRef: TypeRef,
      valueTypeRef: TypeRef,
      values: Set[TypeInvariant]
    ): SetInvariant = new SetInvariant(name, typeRef, valueTypeRef, values)
}

/**
 * @param valueTypeRef the elements type (inside the list)
 */
final class MergedSetsInvariant(
    name: String,
    val valueTypeRef: TypeRef,
    val children: List[TypeInvariant])
    extends TypeInvariant(name, SetRef(valueTypeRef)) {

  private lazy val tupled = Tuple3(name, valueTypeRef, children)

  override def toString = s"MergedSetsInvariant${tupled.toString}"

  override def hashCode: Int = tupled.hashCode

  override def equals(that: Any): Boolean = that match {
    case other: MergedSetsInvariant =>
      this.tupled == other.tupled

    case _ =>
      false
  }
}

object MergedSetsInvariant {

  @inline def apply(
      name: String,
      valueTypeRef: TypeRef,
      children: List[TypeInvariant]
    ): MergedSetsInvariant =
    new MergedSetsInvariant(name, valueTypeRef, children)

  def unapply(
      inv: MergedSetsInvariant
    ): Option[(String, TypeRef, List[TypeInvariant])] = Some(inv.tupled)
}

/**
 * @param typeRef the values type
 */
final class DictionaryInvariant(
    name: String,
    val keyTypeRef: TypeRef,
    val valueTypeRef: TypeRef,
    val entries: Map[TypeInvariant.Simple, TypeInvariant])
    extends TypeInvariant(name, MapRef(keyTypeRef, valueTypeRef)) {
  private lazy val tupled = Tuple4(name, keyTypeRef, valueTypeRef, entries)

  override def toString = s"DictionaryInvariant${tupled.toString}"

  override def hashCode: Int = tupled.hashCode

  override def equals(that: Any): Boolean = that match {
    case other: DictionaryInvariant =>
      this.tupled == other.tupled

    case _ =>
      false
  }
}

object DictionaryInvariant {

  @inline def apply(
      name: String,
      keyTypeRef: TypeRef,
      valueTypeRef: TypeRef,
      entries: Map[TypeInvariant.Simple, TypeInvariant]
    ): DictionaryInvariant =
    new DictionaryInvariant(name, keyTypeRef, valueTypeRef, entries)
}

final class ObjectInvariant(
    name: String,
    typeRef: TypeRef)
    extends TypeInvariant(name, typeRef)
    with SimpleInvariant {

  private lazy val tupled = name -> typeRef

  override def toString = s"ObjectInvariant${tupled.toString}"

  override def hashCode: Int = tupled.hashCode

  override def equals(that: Any): Boolean = that match {
    case other: ObjectInvariant =>
      this.tupled == other.tupled

    case _ =>
      false
  }
}

object ObjectInvariant {

  @inline def apply(
      name: String,
      typeRef: TypeRef
    ): ObjectInvariant = new ObjectInvariant(name, typeRef)

  def unapply(
      inv: ObjectInvariant
    ): Option[(String, TypeRef)] = Some(inv.tupled)

}
