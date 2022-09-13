package io.github.scalats.python

import java.io.PrintStream

import io.github.scalats.core.{
  Settings,
  TypeScriptDeclarationMapper,
  TypeScriptField,
  TypeScriptFieldMapper,
  TypeScriptTypeMapper
}
import io.github.scalats.typescript._

final class PythonDeclarationMapper extends TypeScriptDeclarationMapper {

  def apply(
      parent: TypeScriptDeclarationMapper.Resolved,
      settings: Settings,
      typeMapper: TypeScriptTypeMapper.Resolved,
      fieldMapper: TypeScriptFieldMapper,
      declaration: Declaration,
      out: PrintStream
    ): Option[Unit] = {
    import settings.{ typescriptIndent => indent }

    val typeNaming = settings.typeNaming(settings, _: TypeRef)

    import declaration.name
    val tpeName = typeNaming(declaration.reference)

    def valueRightHand(owner: SingletonDeclaration, v: Value): Unit = {
      val vd = ValueBodyDeclaration(ValueMemberDeclaration(owner, v), v)

      apply(parent, settings, typeMapper, fieldMapper, vd, out).getOrElse(
        parent(vd, out)
      )
    }

    declaration match {
      case iface @ InterfaceDeclaration(
            _,
            fields,
            typeParams,
            superInterface,
            false
          ) =>
        Some {
          out.println(s"# Declare interface ${tpeName}")

          if (typeParams.nonEmpty) {
            out.println()

            typeParams.foreach { tp =>
              out.println(s"${tp} = typing.TypeVar('${tp}')")
            }

            out.println()
            out.println()
          }

          out.print(s"""@dataclass
class ${tpeName}""")

          superInterface match {
            case Some(si) if (!si.union) => {
              out.print(s"(${typeNaming(si.reference)}")

              if (si.typeParams.nonEmpty) {
                out.print(si.typeParams.mkString("[", ", ", "]"))
              }

              out.println("):")
            }

            case None if (typeParams.nonEmpty) =>
              out.println(typeParams.mkString("(typing.Generic[", ", ", "]):"))

            case _ =>
              out.println(":")
          }

          if (fields.isEmpty) {
            out.println(s"${indent}pass")
          } else {
            fields.foreach {
              emitField(settings, fieldMapper, typeMapper, out, iface, _)
            }
          }
        }

      case UnionDeclaration(_, fields, possibilities, None) =>
        Some {
          val ps = possibilities.toList.sortBy(_.name)
          val pst = ps.map { p => p -> typeNaming(p) }

          val union = pst.map(_._2).mkString("typing.Union[", ", ", "]")

          out.println(s"""# Declare union ${tpeName}
${tpeName} = ${union}""")

          val singletons = pst.collect {
            case (SingletonTypeRef(_, _), nme) => nme
          }

          if (singletons.nonEmpty) {
            out.println(s"""

class ${tpeName}Companion:""")

            singletons.zipWithIndex.foreach {
              case (nme, i) =>
                if (i > 0) {
                  out.println()
                }

                out.println(s"""${indent}@classmethod
${indent}def ${nme}(self) -> ${tpeName}:
${indent}${indent}return ${nme.toLowerCase}.${nme}Inhabitant""")
            }
          }

          if (fields.nonEmpty) {
            out.println(s"""
# Fields are ignored: ${fields.map(_.name) mkString ", "}""")
          }
        }

      case _: UnionDeclaration =>
        Some {
          out.println(s"# Not supported: UnionDeclaration '${name}'")
        }

      case decl @ TaggedDeclaration(id, field) =>
        Some {
          val member = TypeScriptField(field.name)
          val tmapper = typeMapper(settings, decl, member, _: TypeRef)

          out.println(s"""# Declare tagged type ${tpeName}
${tpeName} = typing.NewType('${id}', ${tmapper(field.typeRef)})""")
        }

      case decl @ EnumDeclaration(_, possibilities, values) =>
        Some {
          // from enum import Enum
          out.println(s"""# Declare enum ${tpeName}
from enum import Enum


class ${tpeName}(Enum):""")

          possibilities.toList.foreach { v =>
            out.println(s"${indent}${v.toUpperCase} = '${v}'")
          }

          if (values.nonEmpty) {
            val sd = SingletonDeclaration(decl.name, values, None)

            out.println(s"""


class ${tpeName}Invariants:""")

            values.toList.zipWithIndex.foreach {
              case (v, i) =>
                if (i > 0) {
                  out.println()
                }

                parent(ValueMemberDeclaration(sd, v), out)
            }
          }
        }

      case decl @ SingletonDeclaration(name, values, superInterface) =>
        Some {
          // TODO: super
          val member = TypeScriptField(name)
          val tmapper = typeMapper(settings, decl, member, _: TypeRef)

          out.println(s"# Declare singleton ${tpeName}")

          val constDecl = decl.noSuperInterface

          if (values.nonEmpty) {
            values.headOption match {
              case Some(LiteralValue(_, _, raw)) if superInterface.nonEmpty => {
                out.println(s"""${tpeName} = typing.Literal[${raw}]
${tpeName}Inhabitant: ${tpeName} = ${raw}

""")
              }

              case _ =>
                if (superInterface.nonEmpty) {
                  out.println(s"${tpeName} = typing.Literal['${tpeName}']")

                  if (values.isEmpty) {
                    out.println(
                      s"${tpeName}Inhabitant: ${tpeName} = '${tpeName}'"
                    )
                  }
                }
            }

            out.println(s"class ${tpeName}InvariantsFactory:")

            val fmapper =
              fieldMapper(settings, tpeName, _: String, _: TypeRef)

            val vs = values.toList.map { v =>
              v -> fmapper(v.name, v.reference).name
            }

            vs.zipWithIndex.foreach {
              case ((litVal @ LiteralValue(_, tpe, _), vn), i) => {
                if (i > 0) {
                  out.println()
                }

                out.print(s"""${indent}@classmethod
${indent}def ${vn}(self) -> ${tmapper(tpe)}:
${indent}${indent}return """)
                valueRightHand(constDecl, litVal)

                out.println()
              }

              case ((v, vn), i) => {
                if (i > 0) {
                  out.println()
                }

                out.print(s"""${indent}@classmethod
${indent}def ${vn}(self) -> ${tmapper(v.typeRef)}:
${indent}${indent}return """)
                valueRightHand(constDecl, v)

                out.println()
              }
            }

            out.println()
            out.println()

            out.println(s"${tpeName}Invariants = {")

            vs.foreach {
              case (_, vn) =>
                out.println(
                  s"${indent}'${vn}': ${tpeName}InvariantsFactory.${vn}(),"
                )
            }

            out.println('}')
          } else if (superInterface.nonEmpty) {
            out.println(s"${tpeName} = typing.Literal['${tpeName}']")
            out.println(s"${tpeName}Inhabitant: ${tpeName} = '${tpeName}'")
          }
        }

      case decl @ ValueBodyDeclaration(_) => {
        val member = TypeScriptField(decl.owner.name)
        val tmapper = typeMapper(settings, decl, member, _: TypeRef)

        def nestedEmit(vb: ValueBodyDeclaration): Unit =
          vb.value match {
            case LiteralValue(_, t @ TaggedRef(_, _), rawValue) =>
              out.print(s"${tmapper(t)}($rawValue)")

            case LiteralValue(_, _, rawValue) =>
              out.print(rawValue)

            case SelectValue(_, _, qual, term) => {
              val union: Boolean = vb.reference match {
                case _: UnionMemberRef => true
                case _                 => false
              }

              val qualTpeNme = {
                if (union) {
                  val n = tmapper(qual)

                  s"${n.toLowerCase}.${n}Companion"
                } else {
                  tmapper(qual)
                }
              }

              val termNme =
                fieldMapper(settings, qualTpeNme, term, vb.reference).name

              out.print(s"${qualTpeNme}.${termNme}()")
            }

            case ListValue(_, _, _, elements) => {
              out.print("[")

              elements.zipWithIndex.foreach {
                case (e, i) =>
                  if (i > 0) {
                    out.print(", ")
                  }

                  nestedEmit(ValueBodyDeclaration(vb.member, e))
              }

              out.print("]")
            }

            case MergedListsValue(_, _, children) =>
              children.zipWithIndex.foreach {
                case (c, i) =>
                  if (i > 0) {
                    out.print(" + ")
                  }

                  nestedEmit(ValueBodyDeclaration(vb.member, c))
              }

            case SetValue(_, _, _, elements) => {
              out.print("{")

              elements.zipWithIndex.foreach {
                case (e, i) =>
                  if (i > 0) {
                    out.print(", ")
                  }

                  nestedEmit(ValueBodyDeclaration(vb.member, e))
              }

              out.print("}")
            }

            case MergedSetsValue(_, _, children) =>
              children.zipWithIndex.foreach {
                case (c, i) =>
                  if (i > 0) {
                    out.print(".union(")
                  }

                  nestedEmit(ValueBodyDeclaration(vb.member, c))

                  if (i > 0) {
                    out.print(")")
                  }
              }

            case DictionaryValue(_, _, _, entries) => {
              out.print("{")

              // All keys are literal string
              entries.zipWithIndex.foreach {
                case ((key, v), i) =>
                  if (i > 0) {
                    out.print(", ")
                  }

                  nestedEmit(ValueBodyDeclaration(vb.member, key))
                  out.print(": ")
                  nestedEmit(ValueBodyDeclaration(vb.member, v))
              }

              out.print("}")
            }
          }

        Some(nestedEmit(decl))
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
      s"${settings.typescriptIndent}${tsField.name}: ${typeMapper(settings, owner, tsField, member.typeRef)}"
    )
  }
}
