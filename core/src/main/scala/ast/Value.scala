package io.github.scalats.ast

sealed trait Value {

  /** This value name */
  def name: String

  /** This value type */
  def typeRef: TypeRef

  def reference: TypeRef = typeRef
}

sealed trait SimpleValue { _self: Value => }

object Value {
  type Simple = Value with SimpleValue

  def unapply(decl: Value): Option[String] = Option(decl.name)
}

/**
 * A literal [[Value]].
 *
 * @param name the member name
 * @param typeRef the reference for the member type
 * @param rawValue
 */
case class LiteralValue(
    name: String,
    typeRef: TypeRef,
    rawValue: String)
    extends Value
    with SimpleValue

/**
 * @param name the member name
 * @param typeRef the reference for the member type
 */
case class SelectValue(
    name: String,
    typeRef: TypeRef,
    qualifier: TypeRef,
    term: String)
    extends Value
    with SimpleValue

/**
 * A tuple [[Value]].
 *
 * @param name the member name
 * @param elements the list elements
 */
case class TupleValue(
    name: String,
    typeRef: TypeRef, // TODO: Refactor
    values: List[Value])
    extends Value {
  override def reference: TypeRef = typeRef
}

/**
 * A dictionary [[Value]].
 *
 * @param name the member name
 * @param valueTypeRef the reference for the values type
 * @param entries the dictionary entries
 */
case class DictionaryValue(
    name: String,
    keyTypeRef: TypeRef,
    valueTypeRef: TypeRef,
    entries: Map[Value.Simple, Value])
    extends Value {
  def typeRef = MapType(keyTypeRef, valueTypeRef)

  override def reference: TypeRef = TupleRef(List(keyTypeRef, valueTypeRef))
}

/**
 * A list/multi [[Value]].
 *
 * @param name the member name
 * @param valueTypeRef the reference for the elements type
 * @param elements the list elements
 */
case class ListValue(
    name: String,
    typeRef: TypeRef, // TODO: Refactor
    valueTypeRef: TypeRef,
    elements: List[Value])
    extends Value {
  override def reference: TypeRef = valueTypeRef
}

/**
 * A list flatten from merged child lists [[Value]].
 *
 * @param name the member name
 * @param valueTypeRef the reference for the elements type
 * @param children the sub lists
 */
case class MergedListsValue(
    name: String,
    valueTypeRef: TypeRef,
    children: List[Value])
    extends Value {
  val typeRef = ArrayRef(valueTypeRef, false)
  override def reference: TypeRef = valueTypeRef
}

/**
 * A set/multi [[Value]].
 *
 * @param name the member name
 * @param valueTypeRef the reference for the elements type
 * @param elements the set elements
 */
case class SetValue(
    name: String,
    typeRef: TypeRef,
    valueTypeRef: TypeRef,
    elements: Set[Value])
    extends Value {
  override def reference: TypeRef = valueTypeRef
}

/**
 * A list flatten from merged child sets [[Value]].
 *
 * @param name the member name
 * @param valueTypeRef the reference for the elements type
 * @param children the sub sets
 */
case class MergedSetsValue(
    name: String,
    valueTypeRef: TypeRef,
    children: List[Value])
    extends Value {
  val typeRef = ArrayRef(valueTypeRef, false) // TODO: SetRef
  override def reference: TypeRef = valueTypeRef
}

case class SingletonValue(
    name: String,
    typeRef: TypeRef)
    extends Value
