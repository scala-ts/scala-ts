package io.github.scalats.core

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

final class TypeScriptTypeMapperSpec extends AnyFlatSpec with Matchers {
  import TypeScriptModel._
  import TypeScriptTypeMapper._

  lazy val unresolved: Function3[String, String, TypeRef, String] =
    (_, _, _) => "_tpe_"

  it should "map nullable as Option" in {
    nullableAsOption(
      unresolved, "_", "_", NullableType(StringRef)) should equal(
      Some("Option<_tpe_>"))
  }

  it should "map number as string" in {
    numberAsString(
      unresolved, "_", "_", NumberRef) should equal(Some("string"))
  }

  it should "map date as string" in {
    val mapper = dateAsString(unresolved, "_", "_", _: TypeRef)

    mapper(DateRef) should equal(Some("string"))
    mapper(DateTimeRef) should equal(Some("string"))
  }

  it should "chain" in {
    chain(Seq(numberAsString, dateAsString)).foreach { m =>
      val mapper = m(unresolved, "_", "_", _: TypeRef)

      mapper(NumberRef) should equal(Some("string"))
      mapper(DateRef) should equal(Some("string"))
      mapper(DateTimeRef) should equal(Some("string"))
    }
  }
}
