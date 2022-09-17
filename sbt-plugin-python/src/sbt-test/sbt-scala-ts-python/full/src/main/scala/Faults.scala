package io.github.scalats.sbttest

object HttpErrors {
  val NotFound = "404"
  val InternalServerError = "500"
}

object Faults {
  val http = Seq(HttpErrors.NotFound, HttpErrors.InternalServerError)
}
