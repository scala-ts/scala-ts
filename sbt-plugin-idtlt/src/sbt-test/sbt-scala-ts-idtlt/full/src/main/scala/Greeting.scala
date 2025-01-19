package io.github.scalats.sbttest

sealed trait Greeting

object Greeting {
  case object Hello extends Greeting
  case object GoodBye extends Greeting
  case object Hi extends Greeting
  case object Bye extends Greeting

  case class Whatever(word: String) extends Greeting

  val aliases =
    Map[Greeting, Set[Greeting]](Hello -> Set(Hi), GoodBye -> Set(Bye))
}

object Words {
  val start = List[Greeting](Greeting.Hello, Greeting.Hi)
}
