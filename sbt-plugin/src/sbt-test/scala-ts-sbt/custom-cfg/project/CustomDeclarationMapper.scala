package scalats

import java.io.PrintStream

import io.github.scalats.core.{
  Settings,
  TypeScriptDeclarationMapper,
  TypeScriptFieldMapper,
  TypeScriptTypeMapper
}

import io.github.scalats.typescript.{
  Declaration, UnionDeclaration, TypeRef
}

final class CustomDeclarationMapper extends TypeScriptDeclarationMapper {
  def apply(
    settings: Settings,
    typeMapper: TypeScriptTypeMapper.Resolved,
    fieldMapper: TypeScriptFieldMapper,
    declaration: Declaration,
    out: PrintStream): Option[Unit] = declaration match {
    case decl @ UnionDeclaration(name, fields, possibilities, superInterface) =>
      Some {
        val typeNaming = settings.typeNaming(settings, _: TypeRef)

        // Union interface
        out.println("// Custom declaration handling")
        out.println(s"export interface ${typeNaming(decl.reference)} {")

        // Abstract fields - common to all the subtypes
        fields.foreach { member =>
          val tsField = fieldMapper(settings, name, member.name, member.typeRef)

          out.println(s"${settings.typescriptIndent}${tsField.name}: ${typeMapper(name, tsField, member.typeRef)}${settings.typescriptLineSeparator}")
        }

        out.println(s"${settings.typescriptIndent}_additionalField?: string${settings.typescriptLineSeparator}")

        out.println("}")
      }

    case _ =>
      None
  }
}
