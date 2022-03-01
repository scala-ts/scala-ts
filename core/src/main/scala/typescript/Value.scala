package io.github.scalats.typescript

sealed trait Value {

  /** This value name */
  def name: String

  /** This value type */
  def typeRef: TypeRef

  def reference: TypeRef = typeRef
}

object Value {

  def unapply(decl: Value): Option[String] = decl match {
    case v: Value => Some(v.name)

    case _ => None
  }
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

/**
 * A dictionary [[Value]].
 *
 * @param name the member name
 * @param valueTypeRef the reference for the values type
 * @param entries the dictionary entries
 */
case class DictionaryValue(
    name: String,
    typeRef: TypeRef,
    valueTypeRef: TypeRef,
    entries: Map[String, Value])
    extends Value {
  override def reference: TypeRef = valueTypeRef
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
  val typeRef = ArrayRef(valueTypeRef)
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
  val typeRef = ArrayRef(valueTypeRef) // TODO: SetRef
  override def reference: TypeRef = valueTypeRef
}
