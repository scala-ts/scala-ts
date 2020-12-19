package io.github.scalats.core

import java.io.PrintStream

import scala.collection.immutable.Set

import io.github.scalats.typescript.TypeRef

trait TypeScriptPrinter extends Function2[String, Set[TypeRef], PrintStream] {
  /**
   * @param name the name of the type whose declaration must be printed
   * @param requires the type required by the current declaration
   */
  def apply(name: String, requires: Set[TypeRef]): PrintStream
}

object TypeScriptPrinter {
  object StandardOutput extends TypeScriptPrinter {
    def apply(name: String, requires: Set[TypeRef]) = Console.out
  }
}
