package org.scalats.plugins

import scala.collection.immutable.Set

import scala.xml.XML

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

import org.scalats.core.{ Configuration => Settings }

final class ConfigurationSpec extends AnyFlatSpec with Matchers {
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
          excludes = Set(".*Spec", f"ScalaRuntimeFixtures$$", "object:.*ScalaParserResults", "FamilyMember(2|3)")),
        settings = Settings(typescriptIndent = "  ")))
  }
}
