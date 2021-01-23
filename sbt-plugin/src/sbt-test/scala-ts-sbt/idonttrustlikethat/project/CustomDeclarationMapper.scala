package scalats

import java.io.PrintStream

import io.github.scalats.core.{
  Settings,
  TypeScriptDeclarationMapper,
  TypeScriptFieldMapper,
  TypeScriptTypeMapper
}

import io.github.scalats.typescript._

final class CustomDeclarationMapper extends TypeScriptDeclarationMapper {
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

    val name = declaration.name
    val tpeName = typeNaming(declaration.reference)

    def deriving = s"""// Deriving TypeScript type from ${tpeName} validator
export type ${tpeName} = typeof idtlt${tpeName}.T${lineSep}
"""

    declaration match {
      case InterfaceDeclaration(_, fields, Nil, superInterface, false) =>
        Some {
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
$deriving""")
        }

      case i: InterfaceDeclaration =>
        Some {
          out.println(s"// Not supported: InterfaceDeclaration '${name}'")

          if (i.typeParams.nonEmpty) {
            out.println(s"// - type parameters: ${i.typeParams mkString ", "}")
          }
        }

      case UnionDeclaration(_, fields, possibilities, None) =>
        Some {
          out.print(s"""// Validator for UnionDeclaration ${tpeName}
export const idtlt${tpeName} = idtlt.discriminatedUnion(
${indent}'${settings.discriminator.text}', """)

          out.print(possibilities.map { p =>
            s"idtlt${typeNaming(p)}"
          }.toSeq.sorted mkString ", ")

          out.println(s")${lineSep}")

          if (fields.nonEmpty) {
            // TODO: Intersection?

            out.println(s"""
// Fields are ignored: ${fields.map(_.name) mkString ", "}""")
          }

          out.print(s"""
$deriving""")
        }

      case _: UnionDeclaration =>
        Some(out.println(s"// Not supported: UnionDeclaration '${name}'"))

        // TODO: enum WeekDay

      case _ =>
        None
    }
  }

  // ---

  private def emitField(
    settings: Settings,
    fieldMapper: TypeScriptFieldMapper,
    typeMapper: TypeScriptTypeMapper.Resolved,
    o: PrintStream,
    name: String,
    member: Member
  ): Unit = {
    val tsField = fieldMapper(settings, name, member.name, member.typeRef)

    // TODO: scalatsNullableAsOption?

    o.println(s"${settings.typescriptIndent}${tsField.name}: ${typeMapper(settings, name, tsField, member.typeRef)},")
  }
}
