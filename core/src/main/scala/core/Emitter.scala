package io.github.scalats.core

import java.io.PrintStream

import io.github.scalats.ast._

import Internals.ListSet

// TODO: Gather the emit as a default DeclarationMapper.Resolver
/**
 * Default emitter.
 *
 * @param out the function to select a `PrintStream` from type name
 */
final class Emitter( // TODO: Rename
    val settings: Settings,
    out: Emitter.Printer,
    importResolver: Emitter.ImportResolver,
    declarationMapper: Emitter.DeclarationMapper,
    typeMapper: Emitter.TypeMapper) {

  import settings.{ fieldMapper, indent => indent }
  import settings.lineSeparator.{ value => lineSeparator }

  def emit(declarations: ListSet[Declaration]): Unit =
    declarations.foreach {
      case decl: InterfaceDeclaration =>
        emitInterfaceDeclaration(decl)

      case decl: EnumDeclaration =>
        emitEnumDeclaration(decl)

      case decl: SingletonDeclaration =>
        emitSingletonDeclaration(decl)

      case decl: UnionDeclaration =>
        emitUnionDeclaration(decl)

      case decl: TaggedDeclaration =>
        emitTaggedDeclaration(decl)

      case decl: ValueMemberDeclaration =>
        withOut(Declaration.Value, decl.name, requires(decl)) { o =>
          emitValueMember(decl, o)
        }

      case decl: ValueBodyDeclaration =>
        withOut(Declaration.Value, decl.name, requires(decl)) { o =>
          o.print(s"/* Unexpected value body: ${decl.value.name} */")
        }
    }

  // ---

  private val typeNaming = settings.typeNaming(settings, _: TypeRef)

  private val interfaceTypeGuard = Emitter.interfaceTypeGuard(
    _: String,
    _: String,
    _: Iterable[Member],
    { t =>
      val nme = typeNaming(t)
      s"ns${nme}.is${nme}"
    },
    settings
  )

  private val declMapper: Function3[DeclarationMapper.Resolved, Declaration, PrintStream, Option[Unit]] =
    declarationMapper(
      _: DeclarationMapper.Resolved,
      settings,
      resolvedTypeMapper,
      fieldMapper,
      _: Declaration,
      _: PrintStream
    )

  private val requires: ImportResolver.Resolved = { decl =>
    importResolver(decl).getOrElse({
      ImportResolver.defaultResolver(decl)
    })
  }

  private val emitUnionDeclaration: UnionDeclaration => Unit = {
    val default: DeclarationMapper.Resolved = { (decl, o) =>
      decl match {
        case UnionDeclaration(name, fields, _, superInterface) => {
          val tpeName = typeNaming(decl.reference)

          // Union interface
          o.print(s"export interface ${tpeName}")

          superInterface.foreach { iface =>
            o.print(s" extends ${typeNaming(iface.reference)}")
          }

          o.println(" {")

          // Abstract fields - common to all the subtypes
          val fieldList = fields.toList

          fieldList.foreach(emitField(o, decl, _))

          // Type guard
          o.println(s"""}

export function is${tpeName}(v: any): v is ${tpeName} {
${indent}return (""")

          o.println(interfaceTypeGuard(indent + indent, name, fieldList))

          o.println(s"""${indent})${lineSeparator}
}""")
        }

        case decl: ValueMemberDeclaration =>
          emitValueMember(decl, o)

        case decl: ValueBodyDeclaration =>
          emitValueBody(decl, o)

        case _ =>
          o.print(s"/* Unsupported on Union: $decl */")
      }
    }

    { decl =>
      withOut(Declaration.Union, decl.name, requires(decl)) { o =>
        declMapper(default, decl, o).getOrElse(default(decl, o))
      }
    }
  }

  private def emitTaggedDeclaration: TaggedDeclaration => Unit = {
    val default: DeclarationMapper.Resolved = { (decl, o) =>
      decl match {
        case TaggedDeclaration(_, field) => {
          val tpeName = typeNaming(decl.reference)

          val valueType = resolvedTypeMapper(
            settings,
            decl,
            Field(field.name),
            field.typeRef
          )

          o.print(s"export type ${tpeName} = ${valueType}${lineSeparator}")

          val simpleCheck = Emitter.valueCheck(
            "v",
            field.typeRef,
            t => s"is${typeNaming(t)}"
          )

          // Type guard
          o.println(s"""

export function is${tpeName}(v: any): v is ${tpeName} {
${indent}return ${simpleCheck}${lineSeparator}
}""")
        }

        case _ =>
          o.print(s"/* Unsupported on Tagged: $decl */")
      }
    }

    { decl =>
      withOut(Declaration.Tagged, decl.name, requires(decl)) { o =>
        declMapper(default, decl, o).getOrElse(default(decl, o))
      }
    }
  }

  private def emitField(
      o: PrintStream,
      ownerType: Declaration,
      member: Member
    ): Unit = {
    val tsField =
      fieldMapper(settings, ownerType.name, member.name, member.typeRef)

    val nameSuffix: String = {
      if (tsField.flags contains Field.omitable) "?"
      else ""
    }

    o.println(
      s"${indent}${tsField.name}${nameSuffix}: ${resolvedTypeMapper(settings, ownerType, tsField, member.typeRef)}${lineSeparator}"
    )
  }

  private val emitValueBody: (ValueBodyDeclaration, PrintStream) => Unit = {
    val default: DeclarationMapper.Resolved = {
      val tm = { (owner: Declaration, name: String, tpe: TypeRef) =>
        resolvedTypeMapper(
          settings,
          owner,
          Field(name),
          tpe
        )
      }

      { (decl, o) =>
        val nestedEmit = emitValueBody(_: ValueBodyDeclaration, o)

        decl match {
          case vb @ ValueBodyDeclaration(value) => {
            val tpeMapper = tm(vb.owner, value.name, _: TypeRef)

            value match {
              case LiteralValue(_, _, rawValue) =>
                o.print(rawValue)

              case SelectValue(_, _, qual, term) => {
                val qualTpeNme = tpeMapper(qual)
                val termNme =
                  fieldMapper(settings, qualTpeNme, term, vb.reference).name

                o.print(s"${qualTpeNme}.${termNme}")
              }

              case ListValue(_, _, _, elements) => {
                o.print("[ ")

                elements.zipWithIndex.foreach {
                  case (e, i) =>
                    if (i > 0) {
                      o.print(", ")
                    }

                    nestedEmit(ValueBodyDeclaration(vb.member, e))
                }

                o.print(" ]")
              }

              case MergedListsValue(_, _, children) => {
                o.print("[ ...")

                children.zipWithIndex.foreach {
                  case (c, i) =>
                    if (i > 0) {
                      o.print(", ...")
                    }

                    nestedEmit(ValueBodyDeclaration(vb.member, c))
                }

                o.print("]")
              }

              case SetValue(_, _, _, elements) => {
                o.print("new Set([ ")

                elements.zipWithIndex.foreach {
                  case (e, i) =>
                    if (i > 0) {
                      o.print(", ")
                    }

                    nestedEmit(ValueBodyDeclaration(vb.member, e))
                }

                o.print(" ])")
              }

              case MergedSetsValue(_, _, children) => {
                o.print("new Set([ ...")

                children.zipWithIndex.foreach {
                  case (c, i) =>
                    if (i > 0) {
                      o.print(", ...")
                    }

                    nestedEmit(ValueBodyDeclaration(vb.member, c))
                }

                o.print(" ])")
              }

              case d @ DictionaryValue(nme, _, _, entries) => {
                if (
                  entries.forall {
                    case (LiteralValue(_, StringRef, _), _) => true
                    case _                                  => false
                  }
                ) {
                  o.print("{ ")

                  // All keys are literal string
                  entries.zipWithIndex.foreach {
                    case ((key, v), i) =>
                      if (i > 0) {
                        o.print(", ")
                      }

                      nestedEmit(ValueBodyDeclaration(vb.member, key))
                      o.print(": ")
                      nestedEmit(ValueBodyDeclaration(vb.member, v))
                  }

                  o.print(" }")
                } else {
                  val bufNme = s"__buf${scala.math.abs(nme.hashCode)}"

                  o.print(
                    s"(() => { const ${bufNme}: ${tpeMapper(d.typeRef)} = {}; "
                  )

                  entries.foreach {
                    case (key, v) =>
                      o.print(s"${bufNme}[")
                      nestedEmit(ValueBodyDeclaration(vb.member, key))
                      o.print("] = ")
                      nestedEmit(ValueBodyDeclaration(vb.member, v))
                      o.print("; ")
                  }

                  o.print(s"return ${bufNme} })()")
                }
              }
            }
          }

          case _ =>
            o.print(s"/* Unsupported on ValueBody: $decl */")
        }
      }
    }

    { (decl, out) =>
      declMapper(default, decl, out).getOrElse(default(decl, out))
    }
  }

  private val emitValueMember: (ValueMemberDeclaration, PrintStream) => Unit = {
    val default: DeclarationMapper.Resolved = {
      val tm = { (owner: Declaration, name: String, tpe: TypeRef) =>
        resolvedTypeMapper(
          settings,
          owner,
          Field(name),
          tpe
        )
      }

      { (decl, o) =>
        decl match {
          case vd: ValueMemberDeclaration => {
            val tpeMapper = tm(vd.owner, decl.name, _: TypeRef)
            val nme =
              fieldMapper(settings, vd.owner.name, vd.name, vd.reference).name

            vd.value match {
              case l @ ListValue(_, _, tpe, _) => {
                o.print(
                  s"${indent}public $nme: ReadonlyArray<${tpeMapper(tpe)}> = "
                )

                emitValueBody(ValueBodyDeclaration(vd, l), o)

                o.println(lineSeparator)
              }

              case l @ MergedListsValue(_, tpe, _) => {
                o.print(
                  s"${indent}public $nme: ReadonlyArray<${tpeMapper(tpe)}> = "
                )

                emitValueBody(ValueBodyDeclaration(vd, l), o)

                o.println(lineSeparator)
              }

              case s @ SetValue(_, _, tpe, _) => {
                o.print(
                  s"${indent}public $nme: ReadonlySet<${tpeMapper(tpe)}> = "
                )

                emitValueBody(ValueBodyDeclaration(vd, s), o)

                o.println(lineSeparator)
              }

              case l @ MergedSetsValue(_, tpe, _) => {
                o.print(
                  s"${indent}public $nme: ReadonlySet<${tpeMapper(tpe)}> = "
                )

                emitValueBody(ValueBodyDeclaration(vd, l), o)

                o.println(lineSeparator)
              }

              case d @ DictionaryValue(_, _, _, _) => {
                o.print(
                  s"${indent}public readonly ${nme}: ${tpeMapper(d.typeRef)} = "
                )

                emitValueBody(ValueBodyDeclaration(vd, d), o)

                o.println(lineSeparator)
              }

              case v @ (_: SelectValue | _: LiteralValue) => {
                o.print(
                  s"${indent}public ${nme}: ${tpeMapper(v.reference)} = "
                )

                emitValueBody(ValueBodyDeclaration(vd, v), o)

                o.println(lineSeparator)
              }
            }
          }

          case _ =>
            o.print(s"/* Unsupported on ValueMember: $decl */")
        }
      }
    }

    { (decl, out) =>
      declMapper(default, decl, out).getOrElse(default(decl, out))
    }
  }

  private val emitSingletonDeclaration: SingletonDeclaration => Unit = {
    val default: DeclarationMapper.Resolved = { (decl, o) =>
      decl match {
        case sd @ SingletonDeclaration(_, values, superInterface) => {
          val tpeName = typeNaming(decl.reference)

          // Class definition
          o.print(s"export class ${tpeName}")

          superInterface /*.filter(_ => values.isEmpty)*/ .foreach { iface =>
            o.print(s" implements ${typeNaming(iface.reference)}")
          }

          o.println(" {")

          if (values.nonEmpty) {
            values.toList.foreach { v =>
              emitValueMember(ValueMemberDeclaration(sd, v), o)
              o.println()
            }
          }

          o.println(
            s"${indent}private static instance: $tpeName${lineSeparator}\n"
          )

          o.println(s"${indent}private constructor() {}\n")
          o.println(s"${indent}public static getInstance() {")
          o.println(s"${indent}${indent}if (!${tpeName}.instance) {")
          o.println(s"${indent}${indent}${indent}${tpeName}.instance = new ${tpeName}()${lineSeparator}")
          o.println(s"${indent}${indent}}\n")
          o.println(
            s"${indent}${indent}return ${tpeName}.instance${lineSeparator}"
          )
          o.println(s"${indent}}")

          o.println(s"""}

export const ${tpeName}Inhabitant: ${tpeName} = ${tpeName}.getInstance()${lineSeparator}

export function is${tpeName}(v: any): v is ${tpeName} {
${indent}return (v instanceof ${tpeName}) && (v === ${tpeName}Inhabitant)${lineSeparator}
}""")
        }

        case decl: ValueMemberDeclaration =>
          emitValueMember(decl, o)

        case decl: ValueBodyDeclaration =>
          emitValueBody(decl, o)

        case _ =>
          o.print(s"/* Unsupported on Singleton: $decl */")
      }
    }

    { decl =>
      withOut(Declaration.Singleton, decl.name, requires(decl)) { o =>
        declMapper(default, decl, o).getOrElse(default(decl, o))
      }
    }
  }

  private def emitInterfaceDeclaration: InterfaceDeclaration => Unit = {
    val default: DeclarationMapper.Resolved = { (decl, o) =>
      decl match {
        case InterfaceDeclaration(n, fields, typeParams, superInterface, _) => {
          val tpeName = typeNaming(decl.reference)

          o.print(s"export interface ${tpeName}${typeParameters(typeParams)}")

          superInterface.foreach { iface =>
            o.print(s" extends ${typeNaming(iface.reference)}")
          }

          o.println(" {")

          val fieldList = fields.toList

          fieldList.foreach(emitField(o, decl, _))

          // Type guard
          o.println(s"""}

export function is${tpeName}(v: any): v is ${tpeName} {
${indent}return (""")

          o.println(interfaceTypeGuard(indent + indent, n, fieldList))

          o.println(s"""${indent})${lineSeparator}
}""")
        }

        case _ =>
          o.print(s"/* Unsupported on Interface: $decl */")
      }
    }

    { decl =>
      withOut(Declaration.Interface, decl.name, requires(decl)) { o =>
        declMapper(default, decl, o).getOrElse(default(decl, o))
      }
    }
  }

  private val emitEnumDeclaration: EnumDeclaration => Unit = {
    val default: DeclarationMapper.Resolved = { (decl, o) =>
      decl match {
        case decl: ValueMemberDeclaration =>
          emitValueMember(decl, o)

        case EnumDeclaration(_, possibilities, values) => {
          val tpeName = typeNaming(decl.reference)

          // Entries
          o.println(s"const ${tpeName}Entries = {")

          possibilities.foreach { v => o.println(s"  ${v}: '${v}',") }

          o.println(s"}${lineSeparator}")
          o.println()

          // Type
          o.println(s"export type ${tpeName} = keyof (typeof ${tpeName}Entries)${lineSeparator}")
          o.println()

          // Companion
          o.println(s"""export const ${tpeName} = {
${indent}...${tpeName}Entries,
${indent}values: Object.keys(${tpeName}Entries)
} as const${lineSeparator}
""")

          if (values.nonEmpty) {
            val sd = SingletonDeclaration(decl.name, values, None)

            o.println(s"class ${tpeName}Extra {")

            values.toList.zipWithIndex.foreach {
              case (v, i) =>
                if (i > 0) {
                  o.println()
                }

                emitValueMember(ValueMemberDeclaration(sd, v), o)
            }

            o.println(s"""}

export ${tpeName}Invariants = new ${tpeName}Extra()${lineSeparator}
""")
          }

          o.println(s"""export function is${tpeName}(v: any): v is ${tpeName} {
${indent}return ${tpeName}.values.includes(v)${lineSeparator}
}""")
        }

        case _ =>
          o.print(s"/* Unsupported on Enum: $decl */")
      }
    }

    { decl =>
      withOut(Declaration.Enum, decl.name, requires(decl)) { o =>
        declMapper(default, decl, o).getOrElse(default(decl, o))
      }
    }
  }

  // ---

  @inline private def typeParameters(params: List[String]): String =
    if (params.isEmpty) "" else params.mkString("<", ", ", ">")

  private lazy val resolvedTypeMapper: TypeMapper.Resolved = {
    (_settings: Settings,
        ownerType: Declaration,
        member: Field,
        typeRef: TypeRef
      ) =>
      typeMapper(resolvedTypeMapper, _settings, ownerType, member, typeRef)
        .getOrElse(defaultTypeMapping(ownerType, member, typeRef))
  }

  private def defaultTypeMapping(
      ownerType: Declaration,
      member: Field,
      typeRef: TypeRef
    ): String = Emitter.defaultTypeMapping(
    settings,
    member,
    typeRef,
    typeNaming,
    tr = resolvedTypeMapper(settings, ownerType, member, _: TypeRef)
  )

  private def withOut[T](
      decl: Declaration.Kind,
      name: String,
      imports: ListSet[TypeRef]
    )(f: PrintStream => T
    ): T = {
    lazy val print = out(settings, decl, name, imports)

    try {
      val res = f(print)

      print.flush()

      res
    } finally {
      try {
        print.close()
      } catch {
        case scala.util.control.NonFatal(_) =>
      }
    }
  }
}

private[scalats] object Emitter {

  type DeclarationMapper = Function6[
    DeclarationMapper.Resolved,
    Settings,
    TypeMapper.Resolved,
    FieldMapper,
    Declaration,
    PrintStream,
    Option[Unit]
  ]

  type TypeMapper = Function5[
    TypeMapper.Resolved,
    Settings,
    Declaration,
    Field,
    TypeRef,
    Option[String]
  ]

  type ImportResolver = Declaration => Option[ListSet[TypeRef]]

  /* See `TypeScriptPrinter` */
  type Printer =
    Function4[Settings, Declaration.Kind, String, ListSet[TypeRef], PrintStream]

  // ---

  def interfaceTypeGuard(
      indent: String,
      tpeName: String,
      fields: Iterable[Member],
      guardNaming: TypeRef => String,
      settings: Settings
    ): String = {

    val check = fieldCheck(tpeName, _: Member, guardNaming, settings)

    if (fields.isEmpty) {
      s"${indent}typeof v === 'object' && Object.keys(v).length === 0"
    } else {
      fields.map(check).mkString(s"${indent}(", s") &&\n${indent}(", ")")
    }
  }

  def fieldCheck(
      tpeName: String,
      member: Member,
      guardNaming: TypeRef => String,
      settings: Settings
    ): String = {
    import settings.fieldMapper

    val tsField = fieldMapper(settings, tpeName, member.name, member.typeRef)

    valueCheck(s"v['${tsField.name}']", member.typeRef, guardNaming)
  }

  @SuppressWarnings(Array("ListSize"))
  private[core] def valueCheck(
      name: String,
      typeRef: TypeRef,
      guardNaming: TypeRef => String
    ): String =
    typeRef match {
      case TimeRef =>
        s"(typeof ${name}) === 'string'"

      case DateRef | DateTimeRef =>
        s"${name} && (${name} instanceof Date)"

      case SimpleTypeRef(tpe) =>
        s"(typeof ${name}) === '${tpe}'"

      case t @ (CustomTypeRef(_, _) | SingletonTypeRef(_, _) |
          TaggedRef(_, _)) =>
        s"${name} && ${guardNaming(t)}(${name})"

      case ArrayRef(t) =>
        s"Array.isArray(${name}) && ${name}.every(elmt => ${valueCheck("elmt", t, guardNaming)})"

      case SetRef(t) =>
        s"(${name} instanceof Set) && Array.from(${name}).every(elmt => ${valueCheck("elmt", t, guardNaming)})"

      case TupleRef(ts) => {
        def checkElmts = ts.zipWithIndex.map {
          case (t, i) => valueCheck(s"${name}[${i}]", t, guardNaming)
        }.mkString("(", ") && (", ")")

        s"Array.isArray(${name}) && ${name}.length == ${ts.size} && $checkElmts"
      }

      case MapType(keyType, valueType) => {
        def subCheck = valueCheck(s"${name}[key]", valueType, guardNaming)

        s"(typeof ${name}) == 'object' && Object.keys(${name}).every(key => (${valueCheck("key", keyType, guardNaming)}) && ($subCheck))"
      }

      case UnionType(ps) =>
        ps.map { valueCheck(name, _, guardNaming) }.mkString("(", ") || (", ")")

      case NullableType(tpe) =>
        s"!${name} || (${valueCheck(name, tpe, guardNaming)})"

      case _ =>
        "false" // Unprovable pattern (not happen)
    }

  private[scalats] def defaultTypeMapping(
      settings: Settings,
      member: Field,
      typeRef: TypeRef,
      typeNaming: TypeRef => String,
      tr: TypeRef => String
    ): String = typeRef match {
    case _: NumberRef => "number"

    case BooleanRef => "boolean"

    case TimeRef | StringRef => "string"

    case DateRef | DateTimeRef => "Date"

    case ArrayRef(innerType) =>
      s"ReadonlyArray<${tr(innerType)}>"

    case SetRef(innerType) =>
      s"ReadonlySet<${tr(innerType)}>"

    case TupleRef(params) =>
      params.map(tr).mkString("[", ", ", "]")

    case tpe: TaggedRef =>
      typeNaming(tpe)

    case custom @ CustomTypeRef(_, Nil) =>
      typeNaming(custom)

    case singleton @ SingletonTypeRef(_, _) =>
      typeNaming(singleton)

    case custom @ CustomTypeRef(_, params) =>
      s"${typeNaming(custom)}<${params.map(tr).mkString(", ")}>"

    case tpe: SimpleTypeRef =>
      typeNaming(tpe)

    case NullableType(innerType) if settings.optionToNullable =>
      s"(${tr(innerType)} | null)"

    case NullableType(innerType) if (member.flags contains Field.omitable) => {
      // omitable and !optionToNullable
      tr(innerType)
    }

    case NullableType(innerType) =>
      s"(${tr(innerType)} | undefined)"

    case UnionType(possibilities) =>
      possibilities.map(tr).mkString("(", " | ", ")")

    case MapType(keyType, valueType) =>
      s"{ [key: ${tr(keyType)}]: ${tr(valueType)} }"

  }
}
