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

  import DeclarationMapper.Context

  /**
   * @param declarations The declarations grouped by (type) name
   */
  def emit(declarations: Map[String, ListSet[Declaration]]): Unit =
    emit(declarations, Map.empty)

  def emit(
      declarations: Map[String, ListSet[Declaration]],
      context: Context
    ): Unit = declarations.foreach {
    case (name, decls) =>
      decls.headOption.foreach { first =>
        withOut(
          decl = first.kind,
          others = decls.tail.map(_.kind),
          name = name,
          imports = decls.flatMap(requires)
        ) { out =>
          if (decls.tail.nonEmpty) {
            emitCompositeDeclaration(
              decl = CompositeDeclaration(name = name, members = decls),
              context = context,
              out
            )
          } else {
            emitDeclaration(first, context, out)
          }
        }
      }
  }

  private def emitDeclaration(
      decl: Declaration,
      context: Context,
      out: PrintStream
    ): Unit =
    decl match {
      case i: InterfaceDeclaration =>
        emitInterfaceDeclaration(out)(i, context)

      case e: EnumDeclaration =>
        emitEnumDeclaration(out)(e, context)

      case s: SingletonDeclaration =>
        emitSingletonDeclaration(out)(s, context)

      case u: UnionDeclaration =>
        emitUnionDeclaration(out)(u, context)

      case c: CompositeDeclaration =>
        emitCompositeDeclaration(c, context, out)

      case t: TaggedDeclaration =>
        emitTaggedDeclaration(out)(t, context)

      case vm: ValueMemberDeclaration =>
        emitValueMember(vm, context, out)

      case vb: ValueBodyDeclaration =>
        out.print(
          s"/* Unexpected value body: ${vb.value.name} */"
        )
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

  private val declMapper: Function4[DeclarationMapper.Resolved, Declaration, Context, PrintStream, Option[Unit]] =
    declarationMapper(
      _: DeclarationMapper.Resolved,
      settings,
      resolvedTypeMapper,
      fieldMapper,
      _: Declaration,
      _: Context,
      _: PrintStream
    )

  private val requires: ImportResolver.Resolved = { decl =>
    importResolver(decl).getOrElse({
      ImportResolver.defaultResolver(decl)
    })
  }

  private def emitCompositeDeclaration(
      decl: CompositeDeclaration,
      context: Context,
      out: PrintStream
    ): Unit = {
    val default: DeclarationMapper.Resolved = { (decl, ctx, o) =>
      decl match {
        case CompositeDeclaration(_, ms) => {
          val members: ListSet[Declaration] = {
            if (ms.tail.nonEmpty) {
              ms.filter {
                case SingletonDeclaration(_, values, _) =>
                  // Filter out empty companion object
                  values.nonEmpty

                case _ =>
                  true
              }
            } else {
              ms
            }
          }

          members.headOption match {
            case Some(member) if members.tail.isEmpty =>
              emitDeclaration(member, ctx, out)

            case Some(_) => {
              val emitInterface = emitInterfaceDeclaration(o)
              val emitEnum = emitEnumDeclaration(o)
              val emitUnion = emitUnionDeclaration(o)
              val emitTagged = emitTaggedDeclaration(o)
              val emitSingleton = emitSingletonDeclaration(o)

              val emittedTypes = Seq.newBuilder[String]

              members.zipWithIndex.foreach {
                case (member, i) =>
                  if (i > 0) {
                    o.println()
                  }

                  member match {
                    case m: InterfaceDeclaration => {
                      val iface = m.copy(name = s"I${m.name}")

                      emitInterface(iface, ctx)

                      emittedTypes += typeNaming(iface.reference)
                    }

                    case m: EnumDeclaration => {
                      val enu = m.copy(name = s"${m.name}Enum")

                      emitEnum(enu, ctx)

                      emittedTypes += typeNaming(enu.reference)
                    }

                    case m: SingletonDeclaration => {
                      val snme = s"${m.name}Singleton"
                      val single = m.copy(name = snme)

                      emitSingleton(
                        single,
                        ctx + ("noSingletonAlias" -> "true")
                      )

                      o.println(
                        s"""
export const ${m.name}Inhabitant = ${m.name}SingletonInhabitant${lineSeparator}"""
                      )

                      if (members.size == 1) {
                        // Singleton is not a companion object
                        emittedTypes += typeNaming(single.reference)
                      }
                    }

                    case m: UnionDeclaration => {
                      val union = m.copy(name = s"${m.name}Union")

                      emitUnion(union, ctx)

                      emittedTypes += typeNaming(union.reference)
                    }

                    case m: TaggedDeclaration => {
                      val tagged = m.copy(name = s"${m.name}Tagged")

                      emitTagged(tagged, ctx)

                      emittedTypes += typeNaming(tagged.reference)
                    }

                    case decl: ValueMemberDeclaration =>
                      emitValueMember(decl, ctx, o)

                    case decl: CompositeDeclaration =>
                      o.println(
                        s"/* Unexpected nested composite ${decl.name} */"
                      )

                    case decl: ValueBodyDeclaration =>
                      o.print(
                        s"/* Unexpected value body: ${decl.value.name} */"
                      )
                  }
              }

              val tpeName = typeNaming(decl.reference)
              val memberTypes = emittedTypes.result()

              o.print(s"""
export type ${tpeName} = ${memberTypes mkString " & "}${lineSeparator}

export function is${tpeName}(v: any): v is ${tpeName} {
${indent}return (
""")

              memberTypes.zipWithIndex.foreach {
                case (t, i) =>
                  if (i > 0) {
                    o.print(s""" &&
""")
                  }

                  o.print(
                    s"${indent}${indent}is${t.headOption.map(_.toUpper).mkString}${t drop 1}(v)"
                  )
              }

              o.print(s"""
${indent})${lineSeparator}
}""")
            }

            case None =>
          }
        }

        case vm: ValueMemberDeclaration =>
          emitValueMember(vm, ctx, o)

        case vb: ValueBodyDeclaration =>
          emitValueBody(vb, ctx, o)

        case _ =>
          emitDeclaration(decl, ctx, o)
      }
    }

    declMapper(default, decl, context, out).getOrElse(
      default(decl, context, out)
    )
  }

  private def emitUnionDeclaration(
      out: PrintStream
    ): (UnionDeclaration, Context) => Unit = {
    val default: DeclarationMapper.Resolved = { (decl, context, o) =>
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
          emitValueMember(decl, context, o)

        case decl: ValueBodyDeclaration =>
          emitValueBody(decl, context, o)

        case _ =>
          o.print(s"/* Unsupported on Union: $decl */")
      }
    }

    { (decl, context) =>
      declMapper(default, decl, context, out).getOrElse(
        default(decl, context, out)
      )
    }
  }

  private def emitTaggedDeclaration(
      out: PrintStream
    ): (TaggedDeclaration, Context) => Unit = {
    val default: DeclarationMapper.Resolved = { (decl, _, o) =>
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

    { (decl, context) =>
      declMapper(default, decl, context, out).getOrElse(
        default(decl, context, out)
      )
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

  private[core] val emitValueBody: (
      ValueBodyDeclaration,
      Context,
      PrintStream
    ) => Unit = {
    val default: DeclarationMapper.Resolved = {
      val tm = { (owner: Declaration, name: String, tpe: TypeRef) =>
        resolvedTypeMapper(
          settings,
          owner,
          Field(name),
          tpe
        )
      }

      { (decl, context, o) =>
        val nestedEmit = emitValueBody(_: ValueBodyDeclaration, context, o)

        decl match {
          case vb @ ValueBodyDeclaration(value) => {
            val tpeMapper = tm(vb.owner, value.name, _: TypeRef)

            value match {
              case LiteralValue(_, _, rawValue) =>
                o.print(rawValue)

              case SingletonValue(_, vtpe) => {
                val nme = tpeMapper(vtpe)
                val expr = {
                  if (nme.startsWith("ns") && nme.contains('.')) {
                    // Already qualified
                    s"${nme}Inhabitant"
                  } else {
                    s"ns${nme}.${nme}Inhabitant"
                  }
                }

                o.print(expr)
              }

              case SelectValue(_, _, qual, term) => {
                val qualTpeNme = tpeMapper(qual)
                val termNme =
                  fieldMapper(settings, qualTpeNme, term, vb.reference).name

                o.print(s"${qualTpeNme}.${termNme}")
              }

              case TupleValue(_, _, elements) => {
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

                elements.toList.zipWithIndex.foreach {
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

                children.toList.zipWithIndex.foreach {
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
                  o.print("new Map([ ")

                  // All keys are literal string
                  entries.toList.zipWithIndex.foreach {
                    case ((key, v), i) =>
                      if (i > 0) {
                        o.print(", ")
                      }

                      o.print('[')
                      nestedEmit(ValueBodyDeclaration(vb.member, key))
                      o.print(", ")
                      nestedEmit(ValueBodyDeclaration(vb.member, v))
                      o.print(']')
                  }

                  o.print(" ])")
                } else {
                  val bufNme = s"__buf${scala.math.abs(nme.hashCode)}"
                  val tpe = tpeMapper(d.typeRef)

                  val bufTpe: String = {
                    if (tpe startsWith "Readonly<") {
                      tpe.stripPrefix("Readonly<").stripSuffix(">")
                    } else {
                      tpe
                    }
                  }

                  o.print(s"(() => { const ${bufNme}: ${bufTpe} = new Map(); ")

                  entries.foreach {
                    case (key, v) =>
                      o.print(s"${bufNme}.set(")
                      nestedEmit(ValueBodyDeclaration(vb.member, key))
                      o.print(", ")
                      nestedEmit(ValueBodyDeclaration(vb.member, v))
                      o.print("); ")
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

    { (decl, context, out) =>
      declMapper(default, decl, context, out).getOrElse(
        default(decl, context, out)
      )
    }
  }

  private val emitValueMember: (
      ValueMemberDeclaration,
      Context,
      PrintStream
    ) => Unit = {
    val default: DeclarationMapper.Resolved = {
      val tm = { (owner: Declaration, name: String, tpe: TypeRef) =>
        resolvedTypeMapper(
          settings,
          owner,
          Field(name),
          tpe
        )
      }

      { (decl, context, o) =>
        decl match {
          case vd: ValueMemberDeclaration => {
            val tpeMapper = tm(vd.owner, decl.name, _: TypeRef)
            val nme =
              fieldMapper(settings, vd.owner.name, vd.name, vd.reference).name

            vd.value match {
              case t @ TupleValue(_, _, _) => {
                val tpe = t.values
                  .map(v => tpeMapper(v.typeRef))
                  .mkString("[", ", ", "]")

                o.print(
                  s"${indent}public readonly $nme: Readonly<${tpe}> = "
                )

                emitValueBody(ValueBodyDeclaration(vd, t), context, o)

                o.println(lineSeparator)
              }

              case l @ ListValue(_, tpe, _, _) => {
                o.print(
                  s"${indent}public readonly $nme: ${tpeMapper(tpe)} = "
                )

                emitValueBody(ValueBodyDeclaration(vd, l), context, o)

                o.println(lineSeparator)
              }

              case l @ MergedListsValue(_, tpe, _) => {
                o.print(
                  s"${indent}public readonly $nme: ReadonlyArray<${tpeMapper(tpe)}> = "
                )

                emitValueBody(ValueBodyDeclaration(vd, l), context, o)

                o.println(lineSeparator)
              }

              case s @ SetValue(_, tpe, _, _) => {
                o.print(
                  s"${indent}public readonly $nme: ${tpeMapper(tpe)} = "
                )

                emitValueBody(ValueBodyDeclaration(vd, s), context, o)

                o.println(lineSeparator)
              }

              case l @ MergedSetsValue(_, tpe, _) => {
                o.print(
                  s"${indent}public readonly $nme: ReadonlySet<${tpeMapper(tpe)}> = "
                )

                emitValueBody(ValueBodyDeclaration(vd, l), context, o)

                o.println(lineSeparator)
              }

              case d @ DictionaryValue(_, _, _, _) => {
                o.print(
                  s"${indent}public readonly ${nme}: ${tpeMapper(d.typeRef)} = "
                )

                emitValueBody(ValueBodyDeclaration(vd, d), context, o)

                o.println(lineSeparator)
              }

              case s @ SingletonValue(name, ref) => {
                val tpeName = tpeMapper(ref)
                val tpeExpr = {
                  if (tpeName.startsWith("ns") && tpeName.contains('.')) {
                    // Already qualified
                    s"${tpeName}Singleton"
                  } else {
                    s"ns${tpeName}.${tpeName}Singleton"
                  }
                }

                o.print(s"${indent}public readonly ${name}: ${tpeExpr} = ")

                emitValueBody(ValueBodyDeclaration(vd, s), context, o)

                o.println(lineSeparator)
              }

              case v: SelectValue => {
                o.print(s"${indent}public readonly ${nme}: ${tpeMapper(v.reference)} = ")

                emitValueBody(ValueBodyDeclaration(vd, v), context, o)

                o.println(lineSeparator)
              }

              case v: LiteralValue => {
                o.print(
                  s"${indent}public readonly ${nme}: ${tpeMapper(v.reference)} & ${v.rawValue} = "
                )

                emitValueBody(ValueBodyDeclaration(vd, v), context, o)

                o.println(lineSeparator)
              }
            }
          }

          case _ =>
            o.print(s"/* Unsupported on ValueMember: $decl */")
        }
      }
    }

    { (decl, context, out) =>
      declMapper(default, decl, context, out).getOrElse(
        default(decl, context, out)
      )
    }
  }

  private def emitSingletonDeclaration(
      out: PrintStream
    ): (SingletonDeclaration, Context) => Unit = {
    val default: DeclarationMapper.Resolved = { (decl, context, o) =>
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
              emitValueMember(ValueMemberDeclaration(sd, v), context, o)
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

          if (!context.contains("noSingletonAlias")) {
            // For compatiblity with Composite integration

            out.println(s"""
export type ${tpeName}Singleton = ${tpeName}${lineSeparator}""")
          }
        }

        case decl: ValueMemberDeclaration =>
          emitValueMember(decl, context, o)

        case decl: ValueBodyDeclaration =>
          emitValueBody(decl, context, o)

        case _ =>
          o.print(s"/* Unsupported on Singleton: $decl */")
      }
    }

    { (decl, context) =>
      declMapper(default, decl, context, out).getOrElse(
        default(decl, context, out)
      )
    }
  }

  private def emitInterfaceDeclaration(
      out: PrintStream
    ): (InterfaceDeclaration, Context) => Unit = {
    val default: DeclarationMapper.Resolved = { (decl, _, o) =>
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
          if (typeParams.nonEmpty) {
            o.println(s"""}

// No valid type guard for generic interface ${tpeName}""")

          } else {
            o.println(s"""}

export function is${tpeName}(v: any): v is ${tpeName} {
${indent}return (""")

            o.println(interfaceTypeGuard(indent + indent, n, fieldList))

            o.println(s"""${indent})${lineSeparator}
}""")
          }
        }

        case _ =>
          o.print(s"/* Unsupported on Interface: $decl */")
      }
    }

    { (decl, context) =>
      declMapper(default, decl, context, out).getOrElse(
        default(decl, context, out)
      )
    }
  }

  private def emitEnumDeclaration(
      out: PrintStream
    ): (EnumDeclaration, Context) => Unit = {
    val default: DeclarationMapper.Resolved = { (decl, context, o) =>
      decl match {
        case decl: ValueMemberDeclaration =>
          emitValueMember(decl, context, o)

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

                emitValueMember(ValueMemberDeclaration(sd, v), context, o)
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

    { (decl, context) =>
      declMapper(default, decl, context, out).getOrElse(
        default(decl, context, out)
      )
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
      others: ListSet[Declaration.Kind],
      name: String,
      imports: ListSet[TypeRef]
    )(f: PrintStream => T
    ): T = {
    lazy val print = out(settings, decl, others, name, imports)

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

  type DeclarationMapper = Function7[
    DeclarationMapper.Resolved,
    Settings,
    TypeMapper.Resolved,
    FieldMapper,
    Declaration,
    DeclarationMapper.Context,
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
    Function5[Settings, Declaration.Kind, ListSet[
      Declaration.Kind
    ], String, ListSet[TypeRef], PrintStream]

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

      case ArrayRef(t, false) =>
        s"Array.isArray(${name}) && ${name}.every(elmt => ${valueCheck("elmt", t, guardNaming)})"

      case ArrayRef(t, true) =>
        s"Array.isArray(${name}) && ${name}.length > 0 && ${name}.every(elmt => ${valueCheck("elmt", t, guardNaming)})"

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

    case ArrayRef(innerType, false) =>
      s"ReadonlyArray<${tr(innerType)}>"

    case ArrayRef(innerType, true) => {
      val elmTpe = tr(innerType)

      s"readonly [${elmTpe}, ...ReadonlyArray<${elmTpe}>]"
    }

    case SetRef(innerType) =>
      s"ReadonlySet<${tr(innerType)}>"

    case TupleRef(params) =>
      params.map(tr).mkString("[", ", ", "]")

    case tpe @ (TaggedRef(_, _) | CustomTypeRef(_, Nil) |
        SingletonTypeRef(_, _)) =>
      typeNaming(tpe)

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
      s"Readonly<Map<${tr(keyType)}, ${tr(valueType)}>>"

  }
}
