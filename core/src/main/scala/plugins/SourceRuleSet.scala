package io.github.scalats.plugins

import scala.collection.immutable.Set

import scala.xml._

final class SourceRuleSet(
  val includes: Set[String],
  val excludes: Set[String]) {

  override def equals(that: Any): Boolean = that match {
    case other: SourceRuleSet => tupled == other.tupled
    case _ => false
  }

  override def hashCode: Int = tupled.hashCode

  override def toString = s"{ includes: [${includes mkString ", "}], excludes: [${excludes mkString ", "}] }"

  private[plugins] lazy val tupled = includes -> excludes
}

object SourceRuleSet {
  def apply(
    includes: Set[String] = Set.empty,
    excludes: Set[String] = Set.empty): SourceRuleSet =
    new SourceRuleSet(includes, excludes)

  def load(xml: Node): SourceRuleSet = new SourceRuleSet(
    includes = (xml \ "includes" \ "include").map(_.text).toSet,
    excludes = (xml \ "excludes" \ "exclude").map(_.text).toSet)

  @SuppressWarnings(Array("NullParameter"))
  def toXml(ruleSet: SourceRuleSet, name: String): Elem = {
    def elem(n: String, children: Seq[Elem]) =
      new Elem(
        prefix = null,
        label = n,
        attributes1 = scala.xml.Null,
        scope = new scala.xml.NamespaceBinding(null, null, null),
        minimizeEmpty = true,
        children: _*)

    elem(
      name,
      Seq(
        elem("includes", ruleSet.includes.toSeq.map { i =>
          <include>{ i }</include>
        }),
        elem("excludes", ruleSet.excludes.toSeq.map { i =>
          <exclude>{ i }</exclude>
        })))
  }
}
