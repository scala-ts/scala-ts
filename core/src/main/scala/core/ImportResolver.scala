package io.github.scalats.core

import io.github.scalats.ast._

import Internals.ListSet

/**
 * Resolves the imports for TypeScript declarations.
 * Must be consistent with applied [[TypeScriptPrinter]]
 * and [[TypeScriptDeclarationMapper]].
 *
 * The implementations must be class with a no-arg constructor.
 */
trait ImportResolver extends (Declaration => Option[ListSet[TypeRef]]) { self =>

  /**
   * Resolves `Some` required import for the given `declaration`.
   *
   * If `None` is returns, will use the `Defaults` imports.
   */
  def apply(declaration: Declaration): Option[ListSet[TypeRef]]

  def andThen(m: ImportResolver): ImportResolver = new ImportResolver {

    @inline def apply(decl: Declaration): Option[ListSet[TypeRef]] =
      self(decl).orElse(m(decl))
  }
}

object ImportResolver {
  type Resolved = Declaration => ListSet[TypeRef]

  object Defaults extends ImportResolver {
    def apply(declaration: Declaration): Option[ListSet[TypeRef]] = None
  }

  final class UnionWithLiteralSingleton extends ImportResolver {

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
      multi: Seq[ImportResolver]
    ): Option[ImportResolver] = {
    @scala.annotation.tailrec
    def go(
        in: Seq[ImportResolver],
        out: ImportResolver
      ): ImportResolver =
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

    @annotation.tailrec
    @SuppressWarnings(Array("UnsafeTraversableMethods" /*tail*/ ))
    def qualifiers(in: List[Value], out: ListSet[TypeRef]): ListSet[TypeRef] =
      in.headOption match {
        case Some(DictionaryValue(_, _, _, entries)) =>
          qualifiers(
            entries.keySet.toList ::: entries.values.toList ::: in.tail,
            out
          )

        case Some(ListValue(_, _, _, elements)) =>
          qualifiers(elements ::: in.tail, out)

        case Some(SetValue(_, _, _, elements)) =>
          qualifiers(elements.toList ::: in.tail, out)

        case Some(SelectValue(_, _, qual, _)) =>
          qualifiers(
            in.tail,
            out ++ qual.requires.filterNot {
              case SingletonTypeRef(nme, _) =>
                // Singleton can be found as qualifier and as union type
                out.exists(_.name == nme)

              case _ =>
                false
            }
          )

        case Some(_) =>
          qualifiers(in.tail, out)

        case _ =>
          out
      }

    (_: Declaration) match {
      case InterfaceDeclaration(name, fields, _, superInterface, _) =>
        fields
          .flatMap(_.typeRef.requires)
          .filterNot(excludes(name)) ++ superInterface.map {
          (_: InterfaceDeclaration).reference
        }

      case s @ SingletonDeclaration(name, values, superInterface) =>
        values.flatMap { v => defaultResolver(ValueMemberDeclaration(s, v)) }
          .filterNot(excludes(name)) ++ superInterface.map {
          (_: InterfaceDeclaration).reference
        }

      case UnionDeclaration(name, fields, _, superInterface) =>
        fields
          .flatMap(_.typeRef.requires)
          .filterNot(excludes(name)) ++ superInterface.map {
          (_: InterfaceDeclaration).reference
        }

      case vd @ ValueMemberDeclaration(
            v @ (ListValue(_, _, _, _) | SetValue(_, _, _, _))
          ) =>
        qualifiers(List(v), vd.reference.requires)

      case vd @ ValueMemberDeclaration(v @ DictionaryValue(_, _, _, _)) =>
        qualifiers(List(v), vd.reference.requires)

      case ValueMemberDeclaration(SelectValue(_, ref, qual, _)) =>
        qual.requires ++ ref.requires

      case ValueMemberDeclaration(LiteralValue(_, ref, _)) =>
        ref.requires

      case _ =>
        ListSet.empty
    }
  }
}
