package io.github.scalats.sbttest

final class Grade(val value: Int) extends AnyVal

case class CaseClassBar(
    firstName: String,
    lastName: String,
    grade: Grade)
