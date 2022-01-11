package io.github.scalats.core

import java.io.PrintStream

import scala.collection.immutable.ListSet

import io.github.scalats.typescript.{ Declaration, TypeRef }

trait TypeScriptPrinter
    extends Function4[Settings, Declaration.Kind, String, ListSet[
      TypeRef
    ], PrintStream] {

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
      name: String,
      requires: ListSet[TypeRef]
    ): PrintStream
}

object TypeScriptPrinter {

  object StandardOutput extends TypeScriptPrinter {

    def apply(
        configuration: Settings,
        kind: Declaration.Kind,
        name: String,
        requires: ListSet[TypeRef]
      ): PrintStream = Console.out
  }
}
