package io.github.scalats.python

import io.github.scalats.core.Internals.ListSet
import io.github.scalats.core.TypeScriptImportResolver

import io.github.scalats.typescript.{
  Declaration,
  // SingletonDeclaration,
  TypeRef
}

final class PythonSingletonImportResolver extends TypeScriptImportResolver {

  def apply(declaration: Declaration): Option[ListSet[TypeRef]] =
    None
  /*
    declaration match {
      case SingletonDeclaration(name, values, None) =>
        Some(values.flatMap(_.typeRef.requires).filterNot(_.name == name))

      case _ =>
        None
    }*/
}
