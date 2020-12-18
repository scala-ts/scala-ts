package io.github.scalats.core

import scala.xml.XML

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

final class CoreConfigSpec extends AnyFlatSpec with Matchers {
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

  it should "load configuration from fully defined XML" in {
    val cfg = Configuration.load(
      XML loadString fullXml,
      Logger(org.slf4j.LoggerFactory getLogger getClass))

    cfg should equal(Configuration(typescriptIndent = "\t"))
  }

  it should "write configuration as XML" in {
    Configuration.toXml(
      Configuration(typescriptIndent = "\t")).toString should equal(fullXml)
  }

  it should "load configuration with custom field naming" in {
    val source = s"""<scalats>
  <fieldNaming>${classOf[CustomFieldNaming].getName}</fieldNaming>
</scalats>"""

    val cfg = Configuration.load(
      XML loadString source,
      Logger(org.slf4j.LoggerFactory getLogger getClass))

    cfg should equal(Configuration(fieldNaming = new CustomFieldNaming))
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
