package io.github.scalats.core

import Internals.ListSet

final class InternalsSpec extends org.specs2.mutable.Specification {
  "Internals" title

  def wordSet = ListSet("zoo", "alpha", "charlie", "beta")

  "ListSet" should {
    "be iterable orderly" in {
      val res = Seq.newBuilder[String]

      wordSet.toList.foreach {
        res += _
      }

      res.result() must_=== Seq("zoo", "alpha", "charlie", "beta")
    }
  }
}
