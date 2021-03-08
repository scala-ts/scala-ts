package io.github.scalats.idtlt

import java.io.PrintStream

import scala.collection.immutable.ListSet

import io.github.scalats.core.{
  Settings,
  TypeScriptDeclarationMapper,
  TypeScriptFieldMapper,
  TypeScriptTypeMapper
}
import io.github.scalats.typescript._

final class DeclarationMapper extends TypeScriptDeclarationMapper {
  def apply(
    parent: TypeScriptDeclarationMapper.Resolved,
    settings: Settings,
    typeMapper: TypeScriptTypeMapper.Resolved,
    fieldMapper: TypeScriptFieldMapper,
    declaration: Declaration,
    out: PrintStream): Option[Unit] = {
    import settings.{
      typescriptLineSeparator => lineSep,
      typescriptIndent => indent
    }

    val typeNaming = settings.typeNaming(settings, _: TypeRef)

    import declaration.name
    val tpeName = typeNaming(declaration.reference)

    def deriving = s"""// Deriving TypeScript type from ${tpeName} validator
export type ${tpeName} = typeof idtlt${tpeName}.T${lineSep}
"""

    def discrimitedDecl =
      s"""export const idtltDiscriminated${tpeName} = idtlt.intersection(
${indent}idtlt${tpeName},
${indent}idtlt.object({
${indent}${indent}${settings.discriminator.text}: idtlt.literal('${tpeName}')
${indent}})
)${lineSep}

// Deriving TypeScript type from idtltDiscriminated${tpeName} validator
export type Discriminated${tpeName} = typeof idtltDiscriminated${tpeName}.T${lineSep}"""

    def emit(): Unit = declaration match {
      case InterfaceDeclaration(_, fields, Nil, superInterface, false) => {
        out.println(s"""// Validator for InterfaceDeclaration ${tpeName}
export const idtlt${tpeName} = idtlt.object({""")

        // TODO: list(fields).reverse
        fields.foreach {
          emitField(settings, fieldMapper, typeMapper, out, name, _)
        }

        out.println(s"})${lineSep}")

        superInterface.foreach { si =>
          out.println(s"""
// Super-type declaration ${si.name} is ignored""")
        }

        out.print(s"""
$deriving
$discrimitedDecl

export const discriminated${tpeName}: (_: ${tpeName}) => Discriminated${tpeName} = (v: ${tpeName}) => ({ ${settings.discriminator.text}: '${tpeName}', ...v })${lineSep}""")
      }

      case i: InterfaceDeclaration => {
        out.println(s"// Not supported: InterfaceDeclaration '${name}'")

        if (i.typeParams.nonEmpty) {
          out.println(s"// - type parameters: ${i.typeParams mkString ", "}")
        }
      }

      case UnionDeclaration(_, fields, possibilities, None) => {
        out.println(s"""// Validator for UnionDeclaration ${tpeName}
export const idtlt${tpeName} = idtlt.union(""")

        out.print(possibilities.map { p =>
          val n = typeNaming(p)

          s"${indent}ns${n}.idtltDiscriminated${n}"
        }.toSeq.sorted mkString ",\n")

        out.println(s")${lineSep}")

        if (fields.nonEmpty) {
          // TODO: Intersection?

          out.println(s"""
// Fields are ignored: ${fields.map(_.name) mkString ", "}""")
        }

        out.println(s"""
$deriving
$discrimitedDecl

export const idtlt${tpeName}KnownValues: Array<${tpeName}> = [""")

        val knownValues: ListSet[String] = possibilities.flatMap {
          case SingletonTypeRef(_, values) => values.map(_.rawValue)
          case _ => List.empty[String]
        }

        out.println(s"${indent}${knownValues mkString ", "}\n]${lineSep}")
      }

      case _: UnionDeclaration =>
        out.println(s"// Not supported: UnionDeclaration '${name}'")

      case EnumDeclaration(_, values) => {
        out.println(s"""// Validator for EnumDeclaration ${tpeName}
export const idtlt${tpeName} = idtlt.union(""")

        out.print(values.map { v =>
          s"${indent}idtlt.literal('${v}')"
        } mkString ",\n")

        out.println(s""")

$deriving
$discrimitedDecl

export const idtlt${tpeName}Values: Array<${tpeName}> = [""")

        out.print(values.map { v => s"${indent}'${v}'" } mkString ",\n")
        out.println(s"\n]${lineSep}")
      }

      case SingletonDeclaration(_, values, superInterface) => {
        out.print(s"""// Validator for SingletonDeclaration ${tpeName}
export const idtlt${tpeName} = """)

        values.headOption match {
          case Some(Value(_, _, single)) => {
            if (values.tail.isEmpty) {
              out.println(s"idtlt.literal(${single})${lineSep}")
            } else {
              out.println("idtlt.object({")

              values.foreach {
                case Value(nme, _, v) =>
                  out.println(s"${indent}${nme}: idtlt.literal(${v}),")
              }

              out.println(s"})${lineSep}")
            }
          }

          case _ =>
            out.println(s"idtlt.literal('${name}')")

        }

        superInterface.foreach { si =>
          out.print(s"""
// Super-type declaration ${si.name} is ignored""")
        }

        out.print(s"""
export const idtltDiscriminated${tpeName} = idtlt${tpeName};

$deriving""")
      }
    }

    Some(emit())
  }

  // ---

  private def emitField(
    settings: Settings,
    fieldMapper: TypeScriptFieldMapper,
    typeMapper: TypeScriptTypeMapper.Resolved,
    o: PrintStream,
    name: String,
    member: Member): Unit = {
    val tsField = fieldMapper(settings, name, member.name, member.typeRef)

    o.println(s"${settings.typescriptIndent}${tsField.name}: ${typeMapper(settings, name, tsField, member.typeRef)},")
  }
}
