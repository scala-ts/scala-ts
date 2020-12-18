package io.github.scalats.core

import scala.xml.XML

final class CoreConfigSpec extends org.specs2.mutable.Specification {
  "Core configuration" title

  val fullXml = Seq(
    "<scalats>",
    "<emitInterfaces>true</emitInterfaces>",
    "<emitClasses>false</emitClasses>",
    "<emitCodecs>true</emitCodecs>",
    "<optionToNullable>true</optionToNullable>",
    "<optionToUndefined>false</optionToUndefined>",
    "<prependIPrefix>true</prependIPrefix>",
    "<prependEnclosingClassNames>true</prependEnclosingClassNames>",
    "<typescriptIndent>\t</typescriptIndent>",
    "<typescriptLineSeparator>;</typescriptLineSeparator>",
    "<fieldNaming>Identity</fieldNaming>",
    "<discriminator>_type</discriminator>",
    "</scalats>").mkString("")

  "Fully defined configuration" should {
    "be loaded from XML" in {
      val cfg = Configuration.load(
        XML loadString fullXml,
        Logger(org.slf4j.LoggerFactory getLogger getClass))

      cfg must_=== Configuration(typescriptIndent = "\t")
    }

    "be written as XML" in {
      Configuration.toXml(
        Configuration(typescriptIndent = "\t")).toString must_=== fullXml
    }
  }

  "Configuration with custom field naming" should {
    "be loaded from XML" in {
      val source = s"""<scalats>
  <fieldNaming>${classOf[CustomFieldNaming].getName}</fieldNaming>
</scalats>"""

      val cfg = Configuration.load(
        XML loadString source,
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
