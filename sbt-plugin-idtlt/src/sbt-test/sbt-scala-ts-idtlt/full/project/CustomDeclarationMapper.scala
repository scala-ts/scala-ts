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
      out: PrintStream
    ): Option[Unit] = {
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

    def discrimitedObj =
      s"""export const idtltDiscriminated${tpeName} = idtlt.intersection(
${indent}idtlt${tpeName},
${indent}idtlt.object({
${indent}${indent}'${settings.discriminator.text}': idtlt.literal('${tpeName}')
${indent}})
)${lineSep}
"""

    def emit: Unit = declaration match {
      case tagged @ TaggedDeclaration(_, _) =>
        parent(tagged, out)

      case iface @ InterfaceDeclaration(
            _,
            fields,
            Nil,
            superInterface,
            false
          ) => {

        out.println(s"""// Validator for InterfaceDeclaration ${tpeName}
export const idtlt${tpeName} = idtlt.object({""")

        // TODO: list(fields).reverse
        fields.foreach {
          emitField(settings, fieldMapper, typeMapper, out, iface, _)
        }

        out.println(s"})${lineSep}")

        superInterface.foreach { si =>
          out.println(s"""
// Super-type declaration ${si.name} is ignored""")
        }

        out.print(s"""
$discrimitedObj
$deriving""")
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

        out.print(s"""
$discrimitedObj
$deriving""")
      }

      case _: UnionDeclaration =>
        out.println(s"// Not supported: UnionDeclaration '${name}'")

      // TODO: $discrimitedObj
      case EnumDeclaration(_, values) => {
        out.println(s"""// Validator for EnumDeclaration ${tpeName}
export const idtlt${tpeName} = idtlt.union(""")

        out.print(values.map { v =>
          s"${indent}idtlt.literal('${v}')"
        } mkString ",\n")

        out.print(s""")

$discrimitedObj
$deriving""")
      }

      case SingletonDeclaration(_, values, superInterface) => {
        out.print(s"""// Validator for SingletonDeclaration ${tpeName}
export const idtlt${tpeName} = """)

        values.headOption match {
          case Some(LiteralValue(_, _, single)) => {
            if (values.tail.isEmpty) {
              out.println(s"idtlt.literal(${single})${lineSep}")
            } else {
              out.println(s"idtlt.object({")

              values.foreach {
                case LiteralValue(nme, _, v) =>
                  out.println(s"${indent}${nme}: idtlt.literal(${v}),")

                case v =>
                  out.println(s"/* Unsupported: $v */")
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

      case decl =>
        parent(decl, out)
    }

    Some(emit)
  }

  // ---

  private def emitField(
      settings: Settings,
      fieldMapper: TypeScriptFieldMapper,
      typeMapper: TypeScriptTypeMapper.Resolved,
      o: PrintStream,
      owner: Declaration,
      member: Member
    ): Unit = {
    val tsField = fieldMapper(settings, owner.name, member.name, member.typeRef)

    o.println(
      s"${settings.typescriptIndent}${tsField.name}: ${typeMapper(settings, owner, tsField, member.typeRef)},"
    )
  }
}
