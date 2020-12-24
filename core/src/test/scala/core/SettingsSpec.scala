package io.github.scalats.core

import scala.collection.immutable.Set

import io.github.scalats.tsconfig.{ ConfigFactory, ConfigRenderOptions }

final class SettingsSpec extends org.specs2.mutable.Specification {
  "Settings" title

  import ConfigRenderOptions.concise

  val fullConf = """{"discriminator":"_type","emitCodecs":true,"fieldMapper":"Identity","optionToNullable":false,"prependEnclosingClassNames":true,"prependIPrefix":true,"typescriptIndent":"\t","typescriptLineSeparator":";"}"""

  "Fully defined settings" should {
    "be loaded" in {
      val cfg = Settings.load(
        ConfigFactory parseString fullConf,
        Logger(org.slf4j.LoggerFactory getLogger getClass))

      cfg must_=== Settings(typescriptIndent = "\t")
    }

    "be written" in {
      Settings.toConfig(Settings(typescriptIndent = "\t")).
        root().render(concise) must_=== fullConf
    }
  }

  "Settings with custom field naming" should {
    "be loaded" in {
      val source = s"""fieldMapper = "${classOf[CustomFieldMapper].getName}""""

      val cfg = Settings.load(
        ConfigFactory.parseString(source),
        Logger(org.slf4j.LoggerFactory getLogger getClass))

      cfg must_=== Settings(fieldMapper = new CustomFieldMapper)
    }
  }
}

final class CustomFieldMapper extends TypeScriptFieldMapper {
  def apply(
    settings: Settings,
    ownerType: String,
    propertyName: String,
    propertyType: io.github.scalats.typescript.TypeRef) =
    TypeScriptField(s"_${propertyName}", Set.empty)

  override def hashCode: Int = getClass.hashCode

  override def equals(that: Any): Boolean = that match {
    case _: CustomFieldMapper => true
    case _ => false
  }
}
