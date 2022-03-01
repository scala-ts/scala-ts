package io.github.scalats.idtlt

import java.io.PrintStream

import io.github.scalats.core.{
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

    def valueRightHand(owner: SingletonDeclaration, v: Value): Unit = {
      val vd = ValueBodyDeclaration(ValueMemberDeclaration(owner, v), v)

      apply(parent, settings, typeMapper, fieldMapper, vd, out).getOrElse(
        parent(vd, out)
      )
    }

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

    declaration match {
      case iface @ InterfaceDeclaration(
            _,
            fields,
            Nil,
            superInterface,
            false
          ) =>
        Some {
          out.println(s"""// Validator for InterfaceDeclaration ${tpeName}
export const idtlt${tpeName} = idtlt.object({""")

          fields.foreach {
            emitField(settings, fieldMapper, typeMapper, out, iface, _)
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
${interfaceTypeGuard(tpeName, fields.toList)}
${indent})${lineSep}
}""")
        }

      case i: InterfaceDeclaration =>
        Some {
          out.println(s"// Not supported: InterfaceDeclaration '${name}'")

          if (i.typeParams.nonEmpty) {
            out.println(s"// - type parameters: ${i.typeParams mkString ", "}")
          }

          out.println(s"""
export function is${tpeName}(v: any): boolean {
${indent}return false${lineSep}
}""")

        }

      case UnionDeclaration(_, fields, possibilities, None) =>
        Some {
          out.println(s"""// Validator for UnionDeclaration ${tpeName}
export const idtlt${tpeName} = idtlt.union(""")

          val ps = possibilities.toList.sortBy(_.name)
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

          out.print(s"""
$deriving
$discrimitedDecl

export const idtlt${tpeName}KnownValues: Array<${tpeName}> = [
${indent}""")

          ps.flatMap {
            case SingletonTypeRef(nme, values) => {
              val sd = SingletonDeclaration(nme, values, None)

              if (values.headOption.nonEmpty) {
                values.map(v => () => valueRightHand(sd, v))
              } else {
                List(() => out.print(s"'${nme}'"))
              }
            }

            case _ => List.empty[() => Unit]
          }.zipWithIndex.foreach {
            case (print, i) =>
              if (i > 0) {
                out.print(", ")
              }

              print()
          }

          out.println(s"""\n]${lineSep}

export function is${tpeName}(v: any): v is ${tpeName} {
${indent}return (""")

          out.print(pst.map { n =>
            s"${indent}${indent}ns${n}.is${n}(v)"
          } mkString " ||\n")

          out.println(s"""
${indent})${lineSep}
}""")
        }

      case _: UnionDeclaration =>
        Some {
          out.println(s"// Not supported: UnionDeclaration '${name}'")
        }

      case decl @ TaggedDeclaration(id, field) =>
        Some {
          val member = TypeScriptField(field.name)
          val tmapper = typeMapper(settings, decl, member, _: TypeRef)
          val tagged = tmapper(field.typeRef)

          val fieldTpe = TypeScriptEmitter.defaultTypeMapping(
            settings,
            member,
            field.typeRef,
            settings.typeNaming(settings, _),
            tr = tmapper
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

      case EnumDeclaration(_, values) =>
        Some {
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

      case decl @ SingletonDeclaration(_, values, Some(superInterface)) =>
        Some {
          // Singleton as inhabitant of the superInterface

          out.print(s"""// Validator for SingletonDeclaration ${tpeName}
export const idtlt${tpeName} = """)

          values.headOption match {
            case Some(LiteralValue(_, _, single)) => {
              if (values.toList.drop(1).isEmpty) {
                out.println(s"idtlt.literal(${single})${lineSep}")
              } else {
                out.println("idtlt.object({")

                values.foreach {
                  case LiteralValue(nme, _, v) =>
                    out.println(s"${indent}${nme}: idtlt.literal(${v}),")

                  case v =>
                    out.println(s"${indent}/* Unsupported '${v.getClass.getSimpleName}': ${v.name} */")
                }

                out.println(s"})${lineSep}")
              }
            }

            case _ =>
              out.println(s"idtlt.literal('${name}')")

          }

          out.print(s"""
// Super-type declaration ${superInterface.name} is ignored""")

          out.print(s"""
export const idtltDiscriminated${tpeName} = idtlt${tpeName}${lineSep}

$deriving
export const ${tpeName}Inhabitant: ${tpeName} = """)

          // See UnionDeclaration ps.flatMap
          values.headOption match {
            case Some(LiteralValue(_, _, raw)) => {
              // See TypeMapper#IDTLT_TYPE_MAPPER_1
              val constDecl = decl.noSuperInterface

              if (values.size > 1) {
                out.print(s"{\n${indent}")

                values.zipWithIndex.foreach {
                  case (v, i) =>
                    if (i > 0) {
                      out.print(s",\n${indent}")
                    }

                    out.print(s"${v.name}: ")
                    valueRightHand(constDecl, v)
                }

                out.print("\n}")
              } else {
                out.print(raw)
              }
            }

            case _ =>
              out.print(s"'${name}'")
          }

          out.print(s"""${lineSep}

export function is${tpeName}(v: any): v is ${tpeName} {
${indent}return idtlt${tpeName}.validate(v).ok${lineSep}
}""")
        }

      case decl: SingletonDeclaration =>
        Some {
          parent(decl, out)

          out.print(s"""
export const idtlt${tpeName} =
  idtlt.unknown.and(_unknown => idtlt.Err(
    'Cannot validator instance for singleton ${tpeName}'))${lineSep}
""")
        }

      case ValueBodyDeclaration(
            LiteralValue(_, tagged @ TaggedRef(_, _), v)
          ) =>
        Some {
          val n = typeNaming(tagged)

          out.print(s"ns${n}.${n}($v)")
        }

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
      owner: Declaration,
      member: Member
    ): Unit = {
    val tsField = fieldMapper(settings, owner.name, member.name, member.typeRef)

    o.println(
      s"${settings.typescriptIndent}${tsField.name}: ${typeMapper(settings, owner, tsField, member.typeRef)},"
    )
  }
}
