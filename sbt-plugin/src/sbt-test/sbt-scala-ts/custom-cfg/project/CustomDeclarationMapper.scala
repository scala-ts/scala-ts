package scalats

import java.io.PrintStream

import io.github.scalats.core.{
  Settings,
  DeclarationMapper,
  FieldMapper,
  TypeMapper
}

import io.github.scalats.ast.{
  Declaration,
  InterfaceDeclaration,
  UnionDeclaration,
  TypeRef
}

final class CustomDeclarationMapper extends DeclarationMapper {

  def apply(
      parent: DeclarationMapper.Resolved,
      settings: Settings,
      typeMapper: TypeMapper.Resolved,
      fieldMapper: FieldMapper,
      declaration: Declaration,
      context: DeclarationMapper.Context,
      out: PrintStream
    ): Option[Unit] = declaration match {
    case decl @ UnionDeclaration(name, fields, possibilities, superInterface) =>
      Some {
        val typeNaming = settings.typeNaming(settings, _: TypeRef)
        val tpeName = typeNaming(decl.reference)

        // Union interface
        out.println("// Custom declaration handling")
        out.println(s"export interface ${tpeName} {")

        // Abstract fields - common to all the subtypes
        fields.foreach { member =>
          val tsField = fieldMapper(settings, name, member.name, member.typeRef)

          out.println(
            s"${settings.indent}${tsField.name}: ${typeMapper(settings, decl, tsField, member.typeRef)}${settings.lineSeparator}"
          )
        }

        out.println(s"${settings.indent}_additionalField?: string${settings.lineSeparator}")

        out.println(s"""}

export function is${tpeName}(v: any): v is ${tpeName} {
  return !!v // dummy
}""")
      }

    case decl: InterfaceDeclaration =>
      Some(parent(decl, context, out))

    case _ =>
      None
  }
}
