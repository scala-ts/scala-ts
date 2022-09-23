package io.github.scalats.core

import scala.collection.immutable.Set

import io.github.scalats.tsconfig.{ ConfigFactory, ConfigRenderOptions }

final class SettingsSpec extends org.specs2.mutable.Specification {
  "Settings".title

  import ConfigRenderOptions.concise

  val fullConf =
    """{"discriminator":"_type","emitCodecs":true,"fieldMapper":"Identity","indent":"\t","lineSeparator":";","optionToNullable":false,"prependEnclosingClassNames":true,"typeNaming":"Identity"}"""

  "Fully defined settings" should {
    "be loaded" in {
      val cfg = Settings.load(
        ConfigFactory parseString fullConf,
        Logger(org.slf4j.LoggerFactory getLogger getClass)
      )

      cfg must_=== Settings(indent = "\t")
    }

    "be written" in {
      Settings
        .toConfig(Settings(indent = "\t"))
        .root()
        .render(concise) must_=== fullConf
    }
  }

  "Settings with custom field naming" should {
    "be loaded" in {
      val source = s"""fieldMapper = "${classOf[CustomFieldMapper].getName}""""

      val cfg = Settings.load(
        ConfigFactory.parseString(source),
        Logger(org.slf4j.LoggerFactory getLogger getClass)
      )

      cfg must_=== Settings(fieldMapper = new CustomFieldMapper)
    }
  }
}

final class CustomFieldMapper extends FieldMapper {

  def apply(
      settings: Settings,
      ownerType: String,
      propertyName: String,
      propertyType: io.github.scalats.ast.TypeRef
    ) =
    Field(s"_${propertyName}", Set.empty)

  override def hashCode: Int = getClass.hashCode

  override def equals(that: Any): Boolean = that match {
    case _: CustomFieldMapper => true
    case _                    => false
  }
}
