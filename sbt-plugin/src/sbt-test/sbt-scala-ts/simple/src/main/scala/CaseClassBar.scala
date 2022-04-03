package io.github.scalats.sbttest

final class Grade(val value: Int) extends AnyVal

case class CaseClassBar(
    firstName: String,
    lastName: String,
    grade: Grade)

object Constants {
  def code = 1
  val name = "foo"
  val LowerGrade = new Grade(0)

  val list = List(LowerGrade)
  def set = Set("lorem", "ipsum")

  val dict = Map("A" -> "value #1", "B" -> name)

  def listOfDict = Seq(
    Map("title" -> "Foo", "description" -> "..."),
    Map("title" -> "Bar", "description" -> "...")
  )
}
