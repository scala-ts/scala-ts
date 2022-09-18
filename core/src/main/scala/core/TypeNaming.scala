package io.github.scalats.core

import io.github.scalats.ast.TypeRef

trait TypeNaming extends Function2[Settings, TypeRef, String] {

  /** Returns the TypeScript type name for the given declaration. */
  def apply(settings: Settings, tpe: TypeRef): String
}

object TypeNaming {

  /** Use `TypeRef.name` as-is as type name. */
  object Identity extends TypeNaming {
    def apply(settings: Settings, tpe: TypeRef): String = tpe.name
  }
}
