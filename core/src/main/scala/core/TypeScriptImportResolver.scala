package io.github.scalats.core

import io.github.scalats.typescript._

import Internals.ListSet

/**
 * Resolves the imports for TypeScript declarations.
 * Must be consistent with applied [[TypeScriptPrinter]]
 * and [[TypeScriptDeclarationMapper]].
 *
 * The implementations must be class with a no-arg constructor.
 */
trait TypeScriptImportResolver
    extends (Declaration => Option[ListSet[TypeRef]]) { self =>

  /**
   * Resolves `Some` required import for the given `declaration`.
   *
   * If `None` is returns, will use the `Defaults` imports.
   */
  def apply(declaration: Declaration): Option[ListSet[TypeRef]]

  def andThen(m: TypeScriptImportResolver): TypeScriptImportResolver =
    new TypeScriptImportResolver {

      @inline def apply(decl: Declaration): Option[ListSet[TypeRef]] =
        self(decl).orElse(m(decl))
    }
}

object TypeScriptImportResolver {
  type Resolved = Declaration => ListSet[TypeRef]

  object Defaults extends TypeScriptImportResolver {
    def apply(declaration: Declaration): Option[ListSet[TypeRef]] = None
  }

  final class UnionWithLiteralSingleton extends TypeScriptImportResolver {

    def apply(declaration: Declaration): Option[ListSet[TypeRef]] =
      declaration match {
        case InterfaceDeclaration(name, fields, _, superInterface, _)
            if (superInterface.exists(_.union)) =>
          Some(fields.flatMap(_.typeRef.requires).filterNot(_.name == name))

        case SingletonDeclaration(_, _, Some(_)) =>
          // Only if singleton has a superinterface,
          // so it's a union member
          Some(ListSet.empty[TypeRef])

        case UnionDeclaration(_, _, possibilities, _) =>
          Some(possibilities.map { t => (t: TypeRef) })

        case _ =>
          None
      }
  }

  lazy val unionWithLiteralSingleton = new UnionWithLiteralSingleton

  @SuppressWarnings(Array("UnsafeTraversableMethods" /*tail*/ ))
  def chain(
      multi: Seq[TypeScriptImportResolver]
    ): Option[TypeScriptImportResolver] = {
    @scala.annotation.tailrec
    def go(
        in: Seq[TypeScriptImportResolver],
        out: TypeScriptImportResolver
      ): TypeScriptImportResolver =
      in.headOption match {
        case Some(next) => go(in.tail, out.andThen(next))
        case _          => out
      }

    multi.headOption.map { first => go(multi.tail, first) }
  }

  private[scalats] val defaultResolver: Resolved = {
    def excludes(self: String): TypeRef => Boolean = {
      case ThisTypeRef =>
        true

      case t =>
        t.name == self
    }

    (_: Declaration) match {
      case InterfaceDeclaration(name, fields, _, superInterface, _) =>
        fields
          .flatMap(_.typeRef.requires)
          .filterNot(excludes(name)) ++ superInterface.map {
          i: InterfaceDeclaration => i.reference
        }

      case s @ SingletonDeclaration(name, values, superInterface) =>
        values.flatMap { v => defaultResolver(ValueMemberDeclaration(s, v)) }
          .filterNot(excludes(name)) ++ superInterface.map {
          i: InterfaceDeclaration => i.reference
        }

      case UnionDeclaration(name, fields, _, superInterface) =>
        fields
          .flatMap(_.typeRef.requires)
          .filterNot(excludes(name)) ++ superInterface.map {
          i: InterfaceDeclaration => i.reference
        }

      case vd @ ValueMemberDeclaration(ListValue(_, _, _, elements)) =>
        elements.foldLeft[ListSet[TypeRef]](ListSet.empty) {
          case (set, v) =>
            set ++ defaultResolver(ValueMemberDeclaration(vd.owner, v))
        }

      case vd @ ValueMemberDeclaration(SetValue(_, _, _, elements)) =>
        ListSet.empty ++ elements.flatMap { v =>
          defaultResolver(ValueMemberDeclaration(vd.owner, v))
        }

      case vd @ ValueMemberDeclaration(DictionaryValue(_, _, _, entries)) =>
        entries.foldLeft[ListSet[TypeRef]](ListSet.empty) {
          case (set, (_, v)) =>
            val ev = ValueMemberDeclaration(vd.owner, v)
            set ++ defaultResolver(ev)
        }

      case ValueMemberDeclaration(SelectValue(_, ref, qual, _)) =>
        qual.requires ++ ref.requires

      case ValueMemberDeclaration(LiteralValue(_, ref, _)) =>
        ref.requires

      case _ =>
        ListSet.empty
    }
  }
}
