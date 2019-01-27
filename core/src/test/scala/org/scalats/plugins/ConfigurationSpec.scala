package org.scalats.plugins

import scala.collection.immutable.Set

import scala.xml.XML

import org.scalats.core.{ Configuration => Settings }

import org.scalatest.{ FlatSpec, Matchers }

final class ConfigurationSpec extends FlatSpec with Matchers {
  it should "load configuration from fully defined XML" in {
    val xml = XML.load(getClass getResourceAsStream "/plugin-conf.xml")
    val cfg = Configuration.load(xml)

    cfg should equal(
      Configuration(
        compilationRuleSet = SourceRuleSet(
          includes = Set("ScalaParserSpec\\.scala", "Transpiler.*"),
          excludes = Set("foo")),
        typeRuleSet = SourceRuleSet(
          includes = Set("org\\.scalats\\.core\\..*"),
          excludes = Set(".*Spec")),
        settings = Settings()))
  }
}
