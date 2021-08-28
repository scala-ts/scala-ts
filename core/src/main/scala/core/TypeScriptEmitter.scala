package io.github.scalats.core

import java.io.PrintStream

import scala.collection.immutable.ListSet

import io.github.scalats.typescript._

// TODO: (low priority) Use a template engine (velocity?)
/**
 * @param out the function to select a `PrintStream` from type name
 */
final class TypeScriptEmitter(
  val settings: Settings,
  out: TypeScriptEmitter.Printer,
  importResolver: TypeScriptEmitter.ImportResolver,
  declarationMapper: TypeScriptEmitter.DeclarationMapper,
  typeMapper: TypeScriptEmitter.TypeMapper) {

  import Internals.list

  import settings.{ fieldMapper, typescriptIndent => indent }
  import settings.typescriptLineSeparator.{ value => lineSeparator }

  def emit(declarations: ListSet[Declaration]): Unit =
    list(declarations).foreach {
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
    }

  // ---

  private val typeNaming = settings.typeNaming(settings, _: TypeRef)

  private val interfaceTypeGuard = TypeScriptEmitter.interfaceTypeGuard(_: String, _: String, _: Iterable[Member], t => s"is${typeNaming(t)}", settings)

  private val declMapper: Function3[TypeScriptDeclarationMapper.Resolved, Declaration, PrintStream, Option[Unit]] = declarationMapper(_: TypeScriptDeclarationMapper.Resolved, settings, resolvedTypeMapper, fieldMapper, _: Declaration, _: PrintStream)

  private val requires: TypeScriptImportResolver.Resolved = { decl =>
    importResolver(decl).getOrElse(
      TypeScriptImportResolver.defaultResolver(decl))
  }

  private val emitUnionDeclaration: UnionDeclaration => Unit = {
    val default: TypeScriptDeclarationMapper.Resolved = { (decl, o) =>
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
          val fieldList = list(fields)

          fieldList.foreach(emitField(o, name, _))

          // Type guard
          o.println(s"""}

export function is${tpeName}(v: any): v is ${tpeName} {
${indent}return (""")

          o.println(interfaceTypeGuard(indent + indent, name, fieldList))

          o.println(s"""${indent})${lineSeparator}
}""")
        }

        case _ =>
      }
    }

    { decl =>
      withOut(Declaration.Union, decl.name, requires(decl)) { o =>
        declMapper(default, decl, o).getOrElse(default(decl, o))
      }
    }
  }

  private def emitTaggedDeclaration: TaggedDeclaration => Unit = {
    val default: TypeScriptDeclarationMapper.Resolved = { (decl, o) =>
      decl match {
        case TaggedDeclaration(name, field) => {
          val tpeName = typeNaming(decl.reference)

          val valueType = resolvedTypeMapper(
            settings, name, TypeScriptField(field.name), field.typeRef)

          o.print(s"export type ${tpeName} = ${valueType}${lineSeparator}")

          val simpleCheck = TypeScriptEmitter.valueCheck(
            "v", field.typeRef, t => s"is${typeNaming(t)}")

          // Type guard
          o.println(s"""

export function is${tpeName}(v: any): v is ${tpeName} {
${indent}return ${simpleCheck}${lineSeparator}
}""")
        }

        case _ =>
      }
    }

    { decl =>
      withOut(Declaration.Tagged, decl.name, requires(decl)) { o =>
        declMapper(default, decl, o).getOrElse(default(decl, o))
      }
    }
  }

  private def emitField(o: PrintStream, name: String, member: Member): Unit = {
    val tsField = fieldMapper(settings, name, member.name, member.typeRef)

    val nameSuffix: String = {
      if (tsField.flags contains TypeScriptField.omitable) "?"
      else ""
    }

    o.println(s"${indent}${tsField.name}${nameSuffix}: ${resolvedTypeMapper(settings, name, tsField, member.typeRef)}${lineSeparator}")
  }

  private val emitSingletonDeclaration: SingletonDeclaration => Unit = {
    val default: TypeScriptDeclarationMapper.Resolved = { (decl, o) =>
      decl match {
        case SingletonDeclaration(_, values, superInterface) => {
          val tpeName = typeNaming(decl.reference)

          // Class definition
          o.print(s"export class ${tpeName}")

          superInterface /*.filter(_ => values.isEmpty)*/ .foreach { iface =>
            o.print(s" implements ${typeNaming(iface.reference)}")
          }

          o.println(" {")

          if (values.nonEmpty) {
            list(values).foreach {
              case Value(nme, tpe, v) =>
                o.println(
                  s"${indent}public $nme: $tpe = $v${lineSeparator}")
            }

            o.println()
          }

          o.println(s"${indent}private static instance: $tpeName${lineSeparator}\n")

          o.println(s"${indent}private constructor() {}\n")
          o.println(s"${indent}public static getInstance() {")
          o.println(s"${indent}${indent}if (!${tpeName}.instance) {")
          o.println(s"${indent}${indent}${indent}${tpeName}.instance = new ${tpeName}()${lineSeparator}")
          o.println(s"${indent}${indent}}\n")
          o.println(s"${indent}${indent}return ${tpeName}.instance${lineSeparator}")
          o.println(s"${indent}}")

          o.println(s"""}

export function is${tpeName}(v: any): v is ${tpeName} {
${indent}return (v instanceof ${tpeName}) && (v === ${tpeName}.getInstance())${lineSeparator}
}""")
        }

        case _ =>
      }
    }

    { decl =>
      withOut(Declaration.Singleton, decl.name, requires(decl)) { o =>
        declMapper(default, decl, o).getOrElse(default(decl, o))
      }
    }
  }

  private def emitInterfaceDeclaration: InterfaceDeclaration => Unit = {
    val default: TypeScriptDeclarationMapper.Resolved = { (decl, o) =>
      decl match {
        case InterfaceDeclaration(n, fields, typeParams, superInterface, _) => {
          val tpeName = typeNaming(decl.reference)

          o.print(s"export interface ${tpeName}${typeParameters(typeParams)}")

          superInterface.foreach { iface =>
            o.print(s" extends ${typeNaming(iface.reference)}")
          }

          o.println(" {")

          val fieldList = list(fields)

          fieldList.reverse.foreach(emitField(o, n, _))

          // Type guard
          o.println(s"""}

export function is${tpeName}(v: any): v is ${tpeName} {
${indent}return (""")

          o.println(interfaceTypeGuard(indent + indent, n, fieldList))

          o.println(s"""${indent})${lineSeparator}
}""")
        }

        case _ =>
      }
    }

    { decl =>
      withOut(Declaration.Interface, decl.name, requires(decl)) { o =>
        declMapper(default, decl, o).getOrElse(default(decl, o))
      }
    }
  }

  private val emitEnumDeclaration: EnumDeclaration => Unit = {
    val default: TypeScriptDeclarationMapper.Resolved = { (decl, o) =>
      val values: ListSet[String] = decl match {
        case EnumDeclaration(_, values) => values
        case _ => ListSet.empty
      }

      val tpeName = typeNaming(decl.reference)
      val vs = list(values).map(v => s"'${v}'")

      o.println(s"""export type ${tpeName} = ${vs mkString " | "}${lineSeparator}""")
      o.println()
      o.println(s"""export const ${tpeName}Values = ${
        vs.mkString("[ ", ", ", " ]")
      }${lineSeparator}

export function is${tpeName}(v: any): v is ${tpeName} {
${indent}return (""")

      o.print(values.map { v =>
        s"${indent}${indent}v == '${v}'"
      } mkString " ||\n")
      o.println(s"""\n${indent})${lineSeparator}
}""")
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

  private lazy val resolvedTypeMapper: TypeScriptTypeMapper.Resolved = {
    (
    settings: Settings,
    ownerType: String,
    member: TypeScriptField,
    typeRef: TypeRef) =>
      typeMapper(resolvedTypeMapper, settings, ownerType, member, typeRef).
        getOrElse(defaultTypeMapping(ownerType, member, typeRef))
  }

  private def defaultTypeMapping(
    ownerType: String,
    member: TypeScriptField,
    typeRef: TypeRef): String = TypeScriptEmitter.defaultTypeMapping(
    settings, member, typeRef, typeNaming,
    tr = resolvedTypeMapper(settings, ownerType, member, _: TypeRef))

  private def withOut[T](
    decl: Declaration.Kind,
    name: String,
    imports: ListSet[TypeRef])(f: PrintStream => T): T = {
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

private[scalats] object TypeScriptEmitter {
  type DeclarationMapper = Function6[TypeScriptDeclarationMapper.Resolved, Settings, TypeScriptTypeMapper.Resolved, TypeScriptFieldMapper, Declaration, PrintStream, Option[Unit]]

  type TypeMapper = Function5[TypeScriptTypeMapper.Resolved, Settings, String, TypeScriptField, TypeRef, Option[String]]

  type ImportResolver = Declaration => Option[ListSet[TypeRef]]

  /* See `TypeScriptPrinter` */
  type Printer = Function4[Settings, Declaration.Kind, String, ListSet[TypeRef], PrintStream]

  // ---

  def interfaceTypeGuard(
    indent: String,
    tpeName: String,
    fields: Iterable[Member],
    guardNaming: TypeRef => String,
    settings: Settings): String = {

    val check = fieldCheck(tpeName, _: Member, guardNaming, settings)

    if (fields.isEmpty) {
      s"${indent}v === {}"
    } else {
      fields.map(check).mkString(s"${indent}(", s") &&\n${indent}(", ")")
    }
  }

  def fieldCheck(
    tpeName: String,
    member: Member,
    guardNaming: TypeRef => String,
    settings: Settings): String = {
    import settings.fieldMapper

    val tsField = fieldMapper(settings, tpeName, member.name, member.typeRef)

    valueCheck(s"v['${tsField.name}']", member.typeRef, guardNaming)
  }

  @SuppressWarnings(Array("ListSize"))
  private[core] def valueCheck(
    name: String,
    typeRef: TypeRef,
    guardNaming: TypeRef => String): String =
    typeRef match {
      case DateRef | DateTimeRef =>
        s"${name} && (${name} instanceof Date)"

      case SimpleTypeRef(tpe) =>
        s"(typeof ${name}) === '${tpe}'"

      case t @ (CustomTypeRef(_, _) |
        SingletonTypeRef(_, _) |
        TaggedRef(_, _)) =>
        s"${name} && ${guardNaming(t)}(${name})"

      case ArrayRef(t) =>
        s"Array.isArray(${name}) && ${name}.every(elmt => ${valueCheck("elmt", t, guardNaming)})"

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
    member: TypeScriptField,
    typeRef: TypeRef,
    typeNaming: TypeRef => String,
    tr: TypeRef => String): String = typeRef match {
    case NumberRef => "number"

    case BooleanRef => "boolean"

    case StringRef => "string"

    case DateRef | DateTimeRef => "Date"

    case ArrayRef(innerType) =>
      s"ReadonlyArray<${tr(innerType)}>"

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

    case NullableType(innerType) if (
      member.flags contains TypeScriptField.omitable) => {
      // omitable and !optionToNullable
      tr(innerType)
    }

    case NullableType(innerType) =>
      s"(${tr(innerType)} | undefined)"

    case UnionType(possibilities) =>
      possibilities.map(tr).mkString("(", " | ", ")")

    case MapType(keyType, valueType) =>
      s"{ [key: ${tr(keyType)}]: ${tr(valueType)} }" // TODO: Unit test

  }
}
