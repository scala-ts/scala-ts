package io.github.scalats.plugins

import java.net.URL

import scala.collection.immutable.Set

import scala.xml.XML

import io.github.scalats.core.{ Configuration => Settings, FieldNaming, Logger }

final class PluginConfigSpec extends org.specs2.mutable.Specification {
  "Plugin configuration" title

  lazy val logger = Logger(org.slf4j.LoggerFactory getLogger getClass)

  "Configuration" should {
    {
      lazy val xml = XML.loadString("<scalats></scalats>")
      val defaultCfg = Configuration()

      "be loaded from minimal" in {
        Configuration.load(xml, logger, None) must_=== defaultCfg
      }

      "be written XML with defaults" in {
        val xml = (<scalats><compilationRuleSet><includes/><excludes/></compilationRuleSet><typeRuleSet><includes/><excludes/></typeRuleSet><settings><emitInterfaces>true</emitInterfaces><emitClasses>false</emitClasses><emitCodecs>true</emitCodecs><optionToNullable>true</optionToNullable><optionToUndefined>false</optionToUndefined><prependIPrefix>true</prependIPrefix><prependEnclosingClassNames>true</prependEnclosingClassNames><typescriptIndent>  </typescriptIndent><typescriptLineSeparator>;</typescriptLineSeparator><fieldNaming>Identity</fieldNaming><discriminator>_type</discriminator></settings><additionalClasspath/></scalats>)

        Configuration.load(xml, logger, None) must_=== defaultCfg
      }
    }

    {
      lazy val xml = XML.load(getClass getResourceAsStream "/plugin-conf.xml")
      val customConfig = Configuration(
        compilationRuleSet = SourceRuleSet(
          includes = Set("ScalaParserSpec\\.scala", "Transpiler.*"),
          excludes = Set("foo")),
        typeRuleSet = SourceRuleSet(
          includes = Set("org\\.scalats\\.core\\..*"),
          excludes = Set(".*Spec", f"ScalaRuntimeFixtures$$", "object:.*ScalaParserResults", "FamilyMember(2|3)")),
        settings = Settings(
          typescriptIndent = "  ",
          prependIPrefix = false,
          prependEnclosingClassNames = false,
          fieldNaming = FieldNaming.SnakeCase))

      "be loaded from fully defined XML" in {
        val cfg = Configuration.load(xml, logger, None)

        cfg must_=== customConfig
      }

      "be fully defined and written as XML" in {
        val xml = (<scalats><compilationRuleSet><includes><include>ScalaParserSpec\.scala</include><include>Transpiler.*</include></includes><excludes><exclude>foo</exclude></excludes></compilationRuleSet><typeRuleSet><includes><include>org\.scalats\.core\..*</include></includes><excludes><exclude>.*Spec</exclude><exclude>ScalaRuntimeFixtures$</exclude><exclude>object:.*ScalaParserResults</exclude><exclude>FamilyMember(2|3)</exclude></excludes></typeRuleSet><settings><emitInterfaces>true</emitInterfaces><emitClasses>false</emitClasses><emitCodecs>true</emitCodecs><optionToNullable>true</optionToNullable><optionToUndefined>false</optionToUndefined><prependIPrefix>false</prependIPrefix><prependEnclosingClassNames>false</prependEnclosingClassNames><typescriptIndent>  </typescriptIndent><typescriptLineSeparator>;</typescriptLineSeparator><fieldNaming>SnakeCase</fieldNaming><discriminator>_type</discriminator></settings><additionalClasspath/></scalats>)

        Configuration.load(xml, logger, None) must_=== customConfig
      }
    }

    {
      lazy val xml = XML.loadString("<scalats><additionalClasspath><url>file:///tmp/foo1</url><url>file:///tmp/foo2</url></additionalClasspath></scalats>")
      val cfg = Configuration(additionalClasspath = Seq(
        new URL("file:///tmp/foo1"),
        new URL("file:///tmp/foo2")))

      "be loaded from XML with additional classpath" in {
        Configuration.load(xml, logger, None) must_=== cfg
      }

      "be written as XML with additional classpath" in {
        val xml = (<scalats><compilationRuleSet><includes/><excludes/></compilationRuleSet><typeRuleSet><includes/><excludes/></typeRuleSet><settings><emitInterfaces>true</emitInterfaces><emitClasses>false</emitClasses><emitCodecs>true</emitCodecs><optionToNullable>true</optionToNullable><optionToUndefined>false</optionToUndefined><prependIPrefix>true</prependIPrefix><prependEnclosingClassNames>true</prependEnclosingClassNames><typescriptIndent>  </typescriptIndent><typescriptLineSeparator>;</typescriptLineSeparator><fieldNaming>Identity</fieldNaming><discriminator>_type</discriminator></settings><additionalClasspath><url>file:/tmp/foo1</url><url>file:/tmp/foo2</url></additionalClasspath><typeScriptTypeMappers><class>io.github.scalats.core.TypeScriptTypeMapper$Defaults$</class></typeScriptTypeMappers></scalats>)

        Configuration.load(xml, logger, None) must_=== cfg
      }
    }
  }
}
