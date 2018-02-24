package com.mpc.scalats.examples

import com.mpc.scalats.configuration.Config
import com.mpc.scalats.core.TypeScriptGenerator

/**
 * Created by Milosz on 06.12.2016.
 */

trait Vegetation

sealed trait Fruit extends Vegetation {
  def id: Int
}

case class Apple(id: Int, name: String) extends Fruit

case class Pear(id: Int, other: Boolean) extends Fruit

case class UserId(userId: String)

case class Foo[T, Q](a: T, b: List[Q])

case class Bar(b: Foo[String, String], c: List[Foo[Int, String]])

case class Xyz(bars: Option[List[Option[Bar]]])

object GenericsExample {

  def main(args: Array[String]) {
    TypeScriptGenerator.generateFromClassNames(List(classOf[Xyz].getName))(Config())
  }

}
