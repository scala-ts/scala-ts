package io.github.scalats.plugins

import scala.collection.immutable.Set

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

@com.github.ghik.silencer.silent(".*JavaConverters.*")
object SourceRuleSet {
  import scala.collection.JavaConverters._
  import io.github.scalats.tsconfig.{ ConfigFactory, Config }

  def apply(
    includes: Set[String] = Set.empty,
    excludes: Set[String] = Set.empty): SourceRuleSet =
    new SourceRuleSet(includes, excludes)

  def load(conf: Config): SourceRuleSet = {
    @inline def strings(key: String): Iterable[String] =
      conf.getStringList(key).asScala

    new SourceRuleSet(
      includes = strings("includes").toSet,
      excludes = strings("excludes").toSet)
  }

  @SuppressWarnings(Array("NullParameter"))
  def toConfig(ruleSet: SourceRuleSet): Config = {
    import java.util.Arrays
    val repr = new java.util.HashMap[String, Any](2)

    repr.put("includes", Arrays.asList(ruleSet.includes.toSeq: _*))
    repr.put("excludes", Arrays.asList(ruleSet.excludes.toSeq: _*))

    ConfigFactory.parseMap(repr)
  }
}
