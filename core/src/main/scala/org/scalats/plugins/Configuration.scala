package org.scalats.plugins

import scala.collection.immutable.Set

import scala.xml._

import org.scalats.core.{ Configuration => Settings }

/**
 * @param compilationRuleSet the rule set to filter the Scala compilation units
 * @param typeRuleSet the rule set to filter the types from accepted compilation units (see [[compilationRuleSet]])
 */
final class Configuration(
  val settings: Settings,
  val compilationRuleSet: SourceRuleSet,
  val typeRuleSet: SourceRuleSet) {
  override def equals(that: Any): Boolean = that match {
    case other: Configuration => tupled == other.tupled
    case _ => false
  }

  override def hashCode: Int = tupled.hashCode

  // TODO: Improve for pretty-print
  override def toString = s"Configuration${tupled.toString}"

  private[plugins] lazy val tupled =
    Tuple3(settings, compilationRuleSet, typeRuleSet)
}

final class SourceRuleSet(
  val includes: Set[String],
  val excludes: Set[String]) {

  override def equals(that: Any): Boolean = that match {
    case other: SourceRuleSet => tupled == other.tupled
    case _ => false
  }

  override def hashCode: Int = tupled.hashCode

  override def toString = s"SourceRuleSet${tupled.toString}"

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
}

object Configuration {
  def apply(
    settings: Settings = Settings(),
    compilationRuleSet: SourceRuleSet = SourceRuleSet(),
    typeRuleSet: SourceRuleSet = SourceRuleSet()): Configuration =
    new Configuration(settings, compilationRuleSet, typeRuleSet)

  /**
   * Loads the plugin configuration from given XML.
   */
  def load(xml: Elem): Configuration = {
    val settings: Settings = (xml \ "settings") match {
      case e: Elem => Settings.load(e)
      case _ => Settings()
    }

    val compilationRuleSet: SourceRuleSet =
      (xml \ "compilationRuleSet").headOption.fold(
        SourceRuleSet())(SourceRuleSet.load)

    val typeRuleSet: SourceRuleSet =
      (xml \ "typeRuleSet").headOption.fold(
        SourceRuleSet())(SourceRuleSet.load)

    new Configuration(settings, compilationRuleSet, typeRuleSet)
  }
}
