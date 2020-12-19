package io.github.scalats.core

import java.io.PrintStream

import scala.collection.immutable.Set

import io.github.scalats.typescript.{ Declaration, TypeRef }

trait TypeScriptPrinter
  extends Function3[Declaration.Kind, String, Set[TypeRef], PrintStream] {

  /**
   * @param kind the kind of the declaration to be printed
   * @param name the name of the type whose declaration must be printed
   * @param requires the type required by the current declaration
   */
  def apply(
    kind: Declaration.Kind,
    name: String,
    requires: Set[TypeRef]): PrintStream
}

object TypeScriptPrinter {
  object StandardOutput extends TypeScriptPrinter {
    def apply(
      kind: Declaration.Kind,
      name: String,
      requires: Set[TypeRef]) = Console.out
  }
}
