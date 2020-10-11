package org.scalats.core

import scala.xml.XML

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

final class ConfigurationSpec extends AnyFlatSpec with Matchers {
  it should "load configuration from fully defined XML" in {
    val source = """<scalats>
  <emitInterfaces>true</emitInterfaces>
  <emitClasses>false</emitClasses>
  <optionToNullable>true</optionToNullable>
  <optionToUndefined>false</optionToUndefined>
  <prependIPrefix>true</prependIPrefix>
  <typescriptIndent>  </typescriptIndent>
  <emitCodecs>true</emitCodecs>
</scalats>"""

    val cfg = Configuration.load(XML loadString source)

    cfg should equal(Configuration(typescriptIndent = "  "))
  }
}
