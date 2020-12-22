package io.github.scalats.core

import io.github.scalats.tsconfig.{ ConfigFactory, ConfigRenderOptions }

final class CoreConfigSpec extends org.specs2.mutable.Specification {
  "Core configuration" title

  import ConfigRenderOptions.concise

  val fullConf = """{"discriminator":"_type","emitClasses":false,"emitCodecs":true,"emitInterfaces":true,"fieldNaming":"Identity","optionToNullable":true,"optionToUndefined":false,"prependEnclosingClassNames":true,"prependIPrefix":true,"typescriptIndent":"\t","typescriptLineSeparator":";"}"""

  "Fully defined configuration" should {
    "be loaded" in {
      val cfg = Configuration.load(
        ConfigFactory parseString fullConf,
        Logger(org.slf4j.LoggerFactory getLogger getClass))

      cfg must_=== Configuration(typescriptIndent = "\t")
    }

    "be written" in {
      Configuration.toConfig(Configuration(typescriptIndent = "\t")).
        root().render(concise) must_=== fullConf
    }
  }

  "Configuration with custom field naming" should {
    "be loaded" in {
      val source = s"""fieldNaming = "${classOf[CustomFieldNaming].getName}""""

      val cfg = Configuration.load(
        ConfigFactory.parseString(source),
        Logger(org.slf4j.LoggerFactory getLogger getClass))

      cfg must_=== Configuration(fieldNaming = new CustomFieldNaming)
    }
  }
}

final class CustomFieldNaming extends FieldNaming {
  def apply(tpe: String, property: String): String = s"_${property}"

  override def hashCode: Int = getClass.hashCode

  override def equals(that: Any): Boolean = that match {
    case _: CustomFieldNaming => true
    case _ => false
  }
}
