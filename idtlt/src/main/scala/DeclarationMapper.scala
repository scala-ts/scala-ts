package io.github.scalats.idtlt

import java.io.PrintStream

import io.github.scalats.core.{
  Internals,
  Settings,
  TypeScriptDeclarationMapper,
  TypeScriptEmitter,
  TypeScriptField,
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
      out: PrintStream
    ): Option[Unit] = {
    import settings.{
      typescriptLineSeparator => lineSep,
      typescriptIndent => indent
    }

    val typeNaming = settings.typeNaming(settings, _: TypeRef)

    import declaration.name
    val tpeName = typeNaming(declaration.reference)

    val interfaceTypeGuard = TypeScriptEmitter.interfaceTypeGuard(
      indent + indent,
      _: String,
      _: Iterable[Member],
      { t =>
        val tn = typeNaming(t)
        s"ns${tn}.is${tn}"
      },
      settings
    )

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

export const discriminated${tpeName}: (_: ${tpeName}) => Discriminated${tpeName} = (v: ${tpeName}) => ({ ${settings.discriminator.text}: '${tpeName}', ...v })${lineSep}

export function is${tpeName}(v: any): v is ${tpeName} {
${indent}return (
${interfaceTypeGuard(tpeName, fields)}
${indent})${lineSep}
}""")
      }

      case i: InterfaceDeclaration => {
        out.println(s"// Not supported: InterfaceDeclaration '${name}'")

        if (i.typeParams.nonEmpty) {
          out.println(s"// - type parameters: ${i.typeParams mkString ", "}")
        }

        out.println(s"""
export function is${tpeName}(v: any): boolean {
${indent}return false${lineSep}
}""")

      }

      case UnionDeclaration(_, fields, possibilities, None) => {
        out.println(s"""// Validator for UnionDeclaration ${tpeName}
export const idtlt${tpeName} = idtlt.union(""")

        val ps = Internals.list(possibilities).sortBy(_.name)
        val pst = ps.map(typeNaming)

        out.print(pst.map { n =>
          s"${indent}ns${n}.idtltDiscriminated${n}"
        } mkString ",\n")

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

        val knownValues: List[String] = ps.flatMap {
          case SingletonTypeRef(nme, values) => {
            if (values.headOption.nonEmpty) {
              values.map(_.rawValue)
            } else {
              List(s"'${nme}'")
            }
          }

          case _ => List.empty[String]
        }

        out.println(s"""${indent}${knownValues mkString ", "}\n]${lineSep}

export function is${tpeName}(v: any): v is ${tpeName} {
${indent}return (""")

        out.print(pst.map { n =>
          s"${indent}${indent}ns${n}.is${n}(v)"
        } mkString " ||\n")

        out.println(s"""
${indent})${lineSep}
}
""")
      }

      case _: UnionDeclaration =>
        out.println(s"// Not supported: UnionDeclaration '${name}'")

      case TaggedDeclaration(id, field) => {
        val member = TypeScriptField(field.name)
        val tagged = typeMapper(settings, name, member, field.typeRef)

        val fieldTpe = TypeScriptEmitter.defaultTypeMapping(
          settings,
          member,
          field.typeRef,
          settings.typeNaming(settings, _),
          tr = typeMapper(settings, name, member, _)
        )

        out.println(s"""// Validator for TaggedDeclaration ${tpeName}
export type ${tpeName} = ${fieldTpe} & { __tag: '${id}' }${lineSep}

export function ${tpeName}(${field.name}: ${fieldTpe}): ${tpeName} {
  return ${field.name} as ${tpeName}${lineSep}
}

export const idtlt${tpeName} = ${tagged}.tagged<${tpeName}>()${lineSep}

export function is${tpeName}(v: any): v is ${tpeName} {
${indent}return idtlt${tpeName}.validate(v).ok${lineSep}
}""")
      }

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
        out.println(s"""\n]${lineSep}

export function is${tpeName}(v: any): v is ${tpeName} {
${indent} return idtlt${tpeName}.validate(v).ok${lineSep}
}""")
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

        val constValue: String = values.headOption match {
          case Some(Value(_, _, raw)) => {
            if (values.size > 1) {
              values.map { case Value(n, _, r) => s"${n}: $r" }
                .mkString("{ ", ", ", " }")
            } else {
              raw
            }
          }

          case _ =>
            s"'${name}'"
        }

        out.print(s"""
export const idtltDiscriminated${tpeName} = idtlt${tpeName}${lineSep}

$deriving
export const ${tpeName}Inhabitant: ${tpeName} = $constValue${lineSep}

export function is${tpeName}(v: any): v is ${tpeName} {
${indent}return idtlt${tpeName}.validate(v).ok${lineSep}
}""")
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
      member: Member
    ): Unit = {
    val tsField = fieldMapper(settings, name, member.name, member.typeRef)

    o.println(
      s"${settings.typescriptIndent}${tsField.name}: ${typeMapper(settings, name, tsField, member.typeRef)},"
    )
  }
}
