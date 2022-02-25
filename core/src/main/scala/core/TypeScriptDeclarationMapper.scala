package io.github.scalats.core

import java.io.PrintStream

import io.github.scalats.typescript.Declaration

/**
 * The implementations must be class with a no-arg constructor.
 *
 * See:
 * - [[TypeScriptDeclarationMapper.EnumerationAsEnum]]
 */
trait TypeScriptDeclarationMapper
    extends Function6[
      TypeScriptDeclarationMapper.Resolved,
      Settings,
      TypeScriptTypeMapper.Resolved,
      TypeScriptFieldMapper,
      Declaration,
      PrintStream,
      Option[Unit]
    ] { self =>

  /**
   * @param parent the parent declaration mapper
   * @param settings the current settings
   * @param typeMapper the resolved type mapper
   * @param fieldMapper the field mapper
   * @param declaration the transpiled declaration to be emitted
   * @param out the printer to output the TypeScript code
   * @return Some print operation, or None if `declaration` is not handled
   */
  def apply(
      parent: TypeScriptDeclarationMapper.Resolved,
      settings: Settings,
      typeMapper: TypeScriptTypeMapper.Resolved,
      fieldMapper: TypeScriptFieldMapper,
      declaration: Declaration,
      out: PrintStream
    ): Option[Unit]

  def andThen(m: TypeScriptDeclarationMapper): TypeScriptDeclarationMapper =
    new TypeScriptDeclarationMapper {

      @inline def apply(
          parent: TypeScriptDeclarationMapper.Resolved,
          settings: Settings,
          typeMapper: TypeScriptTypeMapper.Resolved,
          fieldMapper: TypeScriptFieldMapper,
          declaration: Declaration,
          out: PrintStream
        ): Option[Unit] =
        self(parent, settings, typeMapper, fieldMapper, declaration, out)
          .orElse(
            m(parent, settings, typeMapper, fieldMapper, declaration, out)
          )
    }

  override def toString: String = getClass.getName
}

object TypeScriptDeclarationMapper {
  import io.github.scalats.typescript.{
    EnumDeclaration,
    InterfaceDeclaration,
    SingletonDeclaration,
    TaggedDeclaration,
    TypeRef,
    TaggedRef,
    UnionDeclaration,
    LiteralValue,
    ValueBodyDeclaration
  }

  type Resolved = Function2[Declaration, PrintStream, Unit]

  // import com.github.ghik.silencer.silent

  object Defaults extends TypeScriptDeclarationMapper {

    def apply(
        parent: Resolved,
        settings: Settings,
        typeMapper: TypeScriptTypeMapper.Resolved,
        fieldMapper: TypeScriptFieldMapper,
        declaration: Declaration,
        out: PrintStream
      ): Option[Unit] = None
  }

  /**
   * Maps `TaggedDeclaration` as TypeScript
   * `<valueType> & { <name>: undefined }`
   * (rather than type alias for `<valueType>`).
   */
  final class ValueClassAsTagged extends TypeScriptDeclarationMapper {

    def apply(
        parent: Resolved,
        settings: Settings,
        typeMapper: TypeScriptTypeMapper.Resolved,
        fieldMapper: TypeScriptFieldMapper,
        declaration: Declaration,
        out: PrintStream
      ): Option[Unit] = {
      import settings.{
        typescriptIndent => indent,
        typescriptLineSeparator => lineSep
      }

      declaration match {
        case ValueBodyDeclaration(
              LiteralValue(_, tagged @ TaggedRef(_, _), v)
            ) => {
          val typeNaming = settings.typeNaming(settings, _: TypeRef)
          val tpeName = typeNaming(tagged)

          Some(out.print(s"${tpeName}($v)"))
        }

        case decl @ TaggedDeclaration(_, field) =>
          Some {
            val typeNaming = settings.typeNaming(settings, _: TypeRef)

            val tpeName = typeNaming(decl.reference)

            val valueType = typeMapper(
              settings,
              decl,
              TypeScriptField(field.name),
              field.typeRef
            )

            out.print(s"export type ${tpeName} = ${valueType}")
            out.print(s" & { __tag: '${tpeName}' }${lineSep}")

            out.println(s"""

export function ${tpeName}(${field.name}: ${valueType}): ${tpeName} {
  return ${field.name} as ${tpeName}
}""")

            // Type guard
            val simpleCheck = TypeScriptEmitter.valueCheck(
              "v",
              field.typeRef,
              t => s"is${typeNaming(t)}"
            )

            out.println(s"""
export function is${tpeName}(v: any): v is ${tpeName} {
${indent}return ${simpleCheck}${lineSep}
}""")
          }

        case _ =>
          None
      }
    }
  }

  lazy val valueClassAsTagged = new ValueClassAsTagged

  /**
   * Maps `EnumDeclaration` as TypeScript `enum`
   * (rather than union type as default).
   */
  final class EnumerationAsEnum extends TypeScriptDeclarationMapper {

    def apply(
        parent: Resolved,
        settings: Settings,
        typeMapper: TypeScriptTypeMapper.Resolved,
        fieldMapper: TypeScriptFieldMapper,
        declaration: Declaration,
        out: PrintStream
      ): Option[Unit] = declaration match {
      case decl @ EnumDeclaration(_, values) =>
        Some {
          val typeNaming = settings.typeNaming(settings, _: TypeRef)
          import settings.{
            typescriptIndent => indent,
            typescriptLineSeparator => lineSep
          }

          val tpeName = typeNaming(decl.reference)

          out.println(s"export enum ${tpeName} {")

          values.toList.zipWithIndex.foreach {
            case (value, idx) =>
              if (idx > 0) {
                out.println(",")
              }

              out.print(s"${indent}${value} = '${value}'")
          }

          out.println()
          out.println(s"""}

export const ${tpeName}Values: Array<${tpeName}> = [""")

          out.print(values.map { v =>
            s"${indent}${tpeName}.${v}"
          } mkString ",\n")
          out.println(s"""\n]${lineSep}

export function is${tpeName}(v: any): v is ${tpeName} {
${indent}return (""")

          out.print(values.map { v =>
            s"${indent}${indent}v == '${v}'"
          } mkString " ||\n")
          out.println(s"""\n${indent})${lineSep}
}""")
        }

      case _ =>
        None
    }
  }

  lazy val enumerationAsEnum = new EnumerationAsEnum()

  /**
   * Maps `SingletonDeclaration` as TypeScript `const` and [[https://www.typescriptlang.org/docs/handbook/literal-types.html literal type]] (super interface is ignored).
   *
   * - If the singleton doesn't declare any value, uses its name as literal.
   * - If the singleton declares a single value, uses it content as literal.
   * - If the singleton declared multiple values, uses them as literal object.
   */
  final class SingletonAsLiteral extends TypeScriptDeclarationMapper {

    def apply(
        parent: Resolved,
        settings: Settings,
        typeMapper: TypeScriptTypeMapper.Resolved,
        fieldMapper: TypeScriptFieldMapper,
        declaration: Declaration,
        out: PrintStream
      ): Option[Unit] = declaration match {
      case decl @ SingletonDeclaration(name, values, _) =>
        Some {
          val constValue: String = values.headOption match {
            case Some(LiteralValue(_, _, raw)) => {
              if (values.size > 1) {
                values.map {
                  case LiteralValue(n, _, r) => s"${n}: $r"
                  case _                     => "/* TODO */"
                }.mkString("{ ", ", ", " }")
              } else {
                raw
              }
            }

            case _ =>
              s"'${name}'"
          }

          import settings.{
            typescriptIndent => indent,
            typescriptLineSeparator => lineSep
          }

          val singleName = settings.typeNaming(settings, decl.reference)

          out.println(
            s"""export const ${singleName}Inhabitant = $constValue${lineSep}

export type ${singleName} = typeof ${singleName}Inhabitant${lineSep}

export function is${singleName}(v: any): v is ${singleName} {
${indent}return ${singleName}Inhabitant == v${lineSep}
}"""
          )
        }

      case _ =>
        None
    }
  }

  lazy val singletonAsLiteral = new SingletonAsLiteral()

  /**
   * Maps `UnionDeclaration` as simple TypeScript [[https://www.typescriptlang.org/docs/handbook/unions-and-intersections.html#union-types union type]]
   * (rather than interface as default).
   *
   * Declared fields and super interface are ignored.
   */
  final class UnionAsSimpleUnion extends TypeScriptDeclarationMapper {

    def apply(
        parent: Resolved,
        settings: Settings,
        typeMapper: TypeScriptTypeMapper.Resolved,
        fieldMapper: TypeScriptFieldMapper,
        declaration: Declaration,
        out: PrintStream
      ): Option[Unit] = declaration match {
      case decl @ UnionDeclaration(_, _, possibilities, _) =>
        Some {
          import settings.{
            typescriptIndent => indent,
            typescriptLineSeparator => lineSep
          }

          val typeNaming = settings.typeNaming(settings, _: TypeRef)
          val tpeName = typeNaming(decl.reference)
          val ps = possibilities.map(typeNaming).toSeq.sorted

          out.print(s"""export type ${tpeName} = ${ps mkString " | "}${lineSep}

export function is${tpeName}(v: any): v is ${tpeName} {
${indent}return (
${indent}${indent}""")

          out.println(
            ps.map { p => s"is${p}(v)" }.mkString(s" ||\n${indent}${indent}")
          )

          out.println(s"""${indent})${lineSep}
}""")
        }

      case decl @ InterfaceDeclaration(
            _,
            _,
            _,
            Some(
              InterfaceDeclaration(_, _, _, _, true)
            ),
            false
          ) => {
        // Ignore super interface for interface member of union
        Some(parent(decl.copy(superInterface = None), out))
      }

      case _ =>
        None
    }
  }

  lazy val unionAsSimpleUnion = new UnionAsSimpleUnion()

  @SuppressWarnings(Array("UnsafeTraversableMethods" /*tail*/ ))
  def chain(
      multi: Seq[TypeScriptDeclarationMapper]
    ): Option[TypeScriptDeclarationMapper] = {
    @scala.annotation.tailrec
    def go(
        in: Seq[TypeScriptDeclarationMapper],
        out: TypeScriptDeclarationMapper
      ): TypeScriptDeclarationMapper = in.headOption match {
      case Some(next) => go(in.tail, out.andThen(next))
      case _          => out
    }

    multi.headOption.map { first => go(multi.tail, first) }
  }
}
