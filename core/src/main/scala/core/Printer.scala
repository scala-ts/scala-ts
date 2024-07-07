package io.github.scalats.core

import java.io.PrintStream

import io.github.scalats.ast.{ Declaration, TypeRef }

import Internals.ListSet

trait Printer
    extends Function5[Settings, Declaration.Kind, ListSet[
      Declaration.Kind
    ], String, ListSet[TypeRef], PrintStream] {

  /**
   * Resolves the printer to be used to the specified type
   * (and its required associated types).
   *
   * @param configuration the generator configuration
   * @param kind the kind of the declaration to be printed
   * @param name the name of the type whose declaration must be printed
   * @param requires the type required by the current declaration
   */
  def apply(
      configuration: Settings,
      kind: Declaration.Kind,
      others: ListSet[Declaration.Kind],
      name: String,
      requires: ListSet[TypeRef]
    ): PrintStream
}

object Printer {

  object StandardOutput extends Printer {

    def apply(
        configuration: Settings,
        kind: Declaration.Kind,
        others: ListSet[Declaration.Kind],
        name: String,
        requires: ListSet[TypeRef]
      ): PrintStream = Console.out
  }
}
