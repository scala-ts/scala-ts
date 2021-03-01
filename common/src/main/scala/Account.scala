package io.github.scalats.demo.model

case class Account(
    userName: UserName,
    contact: Option[ContactName], // Optional property
    usage: Usage.Usage,
    password: String,
    favoriteFoods: Seq[Food])

// Value class
final class UserName(val name: String) extends AnyVal {
  @inline override def toString = name
}

case class AuthenticatedUser(
  name: UserName,
  token: String)

case class ContactName(
    firstName: String,
    lastName: String,
    age: Int)

// Enumeration
object Usage extends Enumeration {
  type Usage = Value
  val Personal, Professional = Value
}

// Sealed family with Singleton objects
//
// Note: Could use Enumeratum
//    => https://github.com/lloydmeta/enumeratum
sealed trait Food {
  def name: String

  @inline override def toString = name
}

object JapaneseSushi extends Food {
  val name = "sushi"
}

object Pizza extends Food {
  def name = "pizza"
}

case class OtherFood(name: String) extends Food

case class Credentials(
  userName: UserName,
  password: String)
