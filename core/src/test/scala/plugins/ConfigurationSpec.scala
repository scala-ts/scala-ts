package io.github.scalats.plugins

import java.net.URI

import scala.collection.immutable.Set

import io.github.scalats.core.{ FieldMapper, Logger, Settings }
import io.github.scalats.tsconfig.{ ConfigFactory, ConfigRenderOptions }

final class ConfigurationSpec extends org.specs2.mutable.Specification {
  "Plugin configuration".title

  lazy val logger = Logger(org.slf4j.LoggerFactory getLogger getClass)

  val compilationRuleSet = SourceRuleSet(
    includes = Set("ScalaParserSpec\\.scala", "Transpiler.*"),
    excludes = Set("foo")
  )

  val typeRuleSet = SourceRuleSet(
    includes = Set("org\\.scalats\\.core\\..*"),
    excludes = Set(
      ".*Spec",
      f"ScalaRuntimeFixtures$$",
      "object:.*ScalaParserResults",
      "FamilyMember(2|3)"
    )
  )

  "Configuration" should {
    {
      val defaultCfg = Configuration()

      "be loaded from minimal" in {
        Configuration.load(
          ConfigFactory.empty(),
          logger,
          None
        ) must_=== defaultCfg
      }

      "be written with defaults" in {
        val source = ConfigFactory.parseString("""settings {
  emitInterfaces = true
  emitClasses = false
  emitCodecs = true
  optionToNullable = false
  prependEnclosingClassNames = true
  indent = "  "
  lineSeparator = ";"
  typeNaming = "Identity"
  fieldMapper = "Identity"
  discriminator = "_dis"
}""")

        Configuration.load(source, logger, None) must_=== defaultCfg
          .withSettings(
            defaultCfg.settings.copy(
              discriminator = new Settings.Discriminator("_dis")
            )
          )

      }
    }

    {
      val customConfig = Configuration(
        compilationRuleSet = compilationRuleSet,
        typeRuleSet = typeRuleSet,
        settings = Settings(
          indent = "  ",
          prependEnclosingClassNames = false,
          fieldMapper = FieldMapper.SnakeCase
        )
      )

      "be loaded from fully defined" in {
        val cfg = Configuration.load(
          ConfigFactory.parseURL(getClass getResource "/plugin.conf"),
          logger,
          None
        )

        cfg must_=== customConfig
      }

      "be fully defined and written" in {
        val source = ConfigFactory.parseString("""
compilationRuleSet {
  includes = [ "ScalaParserSpec\\.scala", "Transpiler.*" ]
  excludes = [ "foo" ]
}

typeRuleSet {
  includes = [ "org\\.scalats\\.core\\..*" ]
  excludes = [ 
    ".*Spec", "ScalaRuntimeFixtures$", 
    "object:.*ScalaParserResults",
    "FamilyMember(2|3)"
  ]
}

settings {
  emitInterfaces = true
  emitClasses = false
  emitCodecs = true
  optionToNullable = false
  prependEnclosingClassNames = false
  indent = "  "
  lineSeparator = ";"
  typeNaming = "Identity"
  fieldMapper = "SnakeCase"
  discriminator = "_type"
}
""")

        Configuration.load(source, logger, None) must_=== customConfig
      }
    }

    {
      lazy val source = ConfigFactory.parseString(
        """additionalClasspath = [ "file:///tmp/foo1", "file:///tmp/foo2" ]"""
      )

      val cfg = Configuration(additionalClasspath =
        Seq(
          new URI("file:///tmp/foo1").toURL,
          new URI("file:///tmp/foo2").toURL
        )
      )

      "be loaded with additional classpath" in {
        Configuration.load(source, logger, None) must_=== cfg
      }

      "be written with additional classpath" in {
        val source = ConfigFactory.parseString("""
settings {
  emitInterfaces = true
  emitClasses = false
  emitCodecs = true
  optionToNullable = false
  prependEnclosingClassNames = true
  indent = "  "
  lineSeparator = ";"
  typeNaming = "Identity"
  fieldMapper = "Identity"
  discriminator = "_type"
}

additionalClasspath = [ "file:/tmp/foo1", "file:/tmp/foo2" ]

typeMappers = [
  "io.github.scalats.core.TypeMapper$Defaults$"
]

importResolvers = [
  "io.github.scalats.core.ImportResolver$Defaults$"
]
""")

        Configuration.load(source, logger, None) must_=== cfg
      }
    }
  }

  // TODO: Import resolver

  "Source rule set" should {
    "be loaded" in {
      val source = ConfigFactory.parseString("""
includes = [ "ScalaParserSpec\\.scala", "Transpiler.*" ]
excludes = [ "foo" ]""")

      SourceRuleSet.load(source) must_=== compilationRuleSet
    }

    "be written" in {
      import ConfigRenderOptions.concise

      SourceRuleSet
        .toConfig(typeRuleSet)
        .root
        .render(
          concise
        ) must_=== """{"excludes":[".*Spec","ScalaRuntimeFixtures$","object:.*ScalaParserResults","FamilyMember(2|3)"],"includes":["org\\.scalats\\.core\\..*"]}"""
    }
  }
}
