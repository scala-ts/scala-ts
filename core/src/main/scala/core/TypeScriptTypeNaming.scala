package io.github.scalats.core

import io.github.scalats.typescript.TypeRef

trait TypeScriptTypeNaming extends Function2[Settings, TypeRef, String] {
  /** Returns the TypeScript type name for the given declaration. */
  def apply(settings: Settings, tpe: TypeRef): String
}

object TypeScriptTypeNaming {
  /** Use `TypeRef.name` as-is as type name. */
  object Identity extends TypeScriptTypeNaming {
    def apply(settings: Settings, tpe: TypeRef): String = tpe.name
  }
}
