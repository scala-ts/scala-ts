package com.mpc.scalats

import com.mpc.scalats.configuration.Config
import com.mpc.scalats.core.{ Logger, TypeScriptGenerator }

case class Foo[T, Q](a : T, b: List[Q])

case class Bar(b: Foo[String, String], c:List[Foo[Int, String]])

case class Xyz(bars: Option[List[Option[Bar]]])

object GenericsExample {
  def main(args: Array[String]): Unit = {
    val logger = Logger(
      org.slf4j.LoggerFactory getLogger TypeScriptGenerator.getClass)

    TypeScriptGenerator.generateFromClassNames(
      List("com.mpc.scalats.Xyz"), logger)(Config())
  }

}
