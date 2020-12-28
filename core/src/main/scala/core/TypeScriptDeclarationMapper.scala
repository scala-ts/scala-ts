package io.github.scalats.core

import java.io.PrintStream

import io.github.scalats.typescript.Declaration

/**
 * The implementations must be class with a no-arg constructor.
 *
 * See:
 * - [[TypeScriptDeclarationMapper.EnumerationAsEnum]]
 */
trait TypeScriptDeclarationMapper extends Function5[Settings, TypeScriptTypeMapper.Resolved, TypeScriptFieldMapper, Declaration, PrintStream, Option[Unit]] { self =>
  /**
   * @param settings the current settings
   * @param typeMapper the resolved type mapper
   * @param fieldMapper the field mapper
   * @param declaration the transpiled declaration to be emitted
   * @param out the printer to output the TypeScript code
   * @return Some print operation, or None if `declaration` is not handled
   */
  def apply(
    settings: Settings,
    typeMapper: TypeScriptTypeMapper.Resolved,
    fieldMapper: TypeScriptFieldMapper,
    declaration: Declaration,
    out: PrintStream): Option[Unit]

  def andThen(m: TypeScriptDeclarationMapper): TypeScriptDeclarationMapper =
    new TypeScriptDeclarationMapper {
      @inline def apply(
        settings: Settings,
        typeMapper: TypeScriptTypeMapper.Resolved,
        fieldMapper: TypeScriptFieldMapper,
        declaration: Declaration,
        out: PrintStream): Option[Unit] =
        self(settings, typeMapper, fieldMapper, declaration, out).
          orElse(m(settings, typeMapper, fieldMapper, declaration, out))
    }
}

object TypeScriptDeclarationMapper {
  import io.github.scalats.typescript.{ EnumDeclaration, TypeRef }
  import Internals.list

  //import com.github.ghik.silencer.silent

  object Defaults extends TypeScriptDeclarationMapper {
    def apply(
      settings: Settings,
      typeMapper: TypeScriptTypeMapper.Resolved,
      fieldMapper: TypeScriptFieldMapper,
      declaration: Declaration,
      out: PrintStream): Option[Unit] = None
  }

  /**
   * Maps `EnumDeclaration` as TypeScript `enum`
   * (rather than union type as default).
   */
  final class EnumerationAsEnum extends TypeScriptDeclarationMapper {
    def apply(
      settings: Settings,
      typeMapper: TypeScriptTypeMapper.Resolved,
      fieldMapper: TypeScriptFieldMapper,
      declaration: Declaration,
      out: PrintStream): Option[Unit] = declaration match {
      case decl @ EnumDeclaration(_, values) => Some {
        val typeNaming = settings.typeNaming(settings, _: TypeRef)
        import settings.{ typescriptIndent => indent }

        out.println(s"export enum ${typeNaming(decl.reference)} {")

        list(values).zipWithIndex.foreach {
          case (value, idx) =>
            if (idx > 0) {
              out.println(",")
            }

            out.print(s"${indent}${value} = '${value}'")
        }

        out.println()
        out.println("}")
      }

      case _ =>
        None
    }
  }

  lazy val enumerationAsEnum = new EnumerationAsEnum()

  def chain(multi: Seq[TypeScriptDeclarationMapper]): Option[TypeScriptDeclarationMapper] = {
    @scala.annotation.tailrec def go(
      in: Seq[TypeScriptDeclarationMapper],
      out: TypeScriptDeclarationMapper): TypeScriptDeclarationMapper = in.headOption match {
      case Some(next) => go(in.tail, out.andThen(next))
      case _ => out
    }

    multi.headOption.map { first =>
      go(multi.tail, first)
    }
  }
}
