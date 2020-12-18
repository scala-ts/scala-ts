package io.github.scalats.core

import scala.collection.immutable.ListSet

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

final class InternalsSpec extends AnyFlatSpec with Matchers {
  def wordSet = ListSet("zoo", "alpha", "charlie", "beta")

  it should "support order for ListSet" in {
    val res = Seq.newBuilder[String]

    Internals.list(wordSet).foreach {
      res += _
    }

    res.result() should equal(Seq("zoo", "alpha", "charlie", "beta"))
  }
}
