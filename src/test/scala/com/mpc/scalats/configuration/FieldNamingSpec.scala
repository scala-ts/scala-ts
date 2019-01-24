package com.mpc.scalats.configuration

import org.scalatest.{ FlatSpec, Matchers }
import org.scalatest.prop.TableDrivenPropertyChecks._

final class FieldNamingSpec extends FlatSpec with Matchers {
  it should "support identity" in {
    import FieldNaming.Identity

    val fixtures = Table(
      "lorem" -> "lorem",
      "fooBar" -> "fooBar",
      "Ipsum" -> "Ipsum"
    )

    forAll(fixtures) { (name, encoded) =>
      Identity(name) should equal(encoded)
    }
  }

  it should "support snake case" in {
    import FieldNaming.SnakeCase

    val fixtures = Table(
      "lorem" -> "lorem",
      "fooBar" -> "foo_bar",
      "Ipsum" -> "Ipsum"
    )

    forAll(fixtures) { (name, encoded) =>
      SnakeCase(name) should equal(encoded)
    }
  }
}
