package io.github.scalats.core

import java.io.PrintStream

import scala.collection.immutable.{ ListSet, Set }

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
    }

  // ---

  private val typeNaming = settings.typeNaming(settings, _: TypeRef)

  private val declMapper: Function3[TypeScriptDeclarationMapper.Resolved, Declaration, PrintStream, Option[Unit]] = declarationMapper(_: TypeScriptDeclarationMapper.Resolved, settings, resolvedTypeMapper, fieldMapper, _: Declaration, _: PrintStream)

  private val requires: TypeScriptImportResolver.Resolved = { decl =>
    importResolver(decl).getOrElse(
      TypeScriptImportResolver.defaultResolver(decl))
  }

  private val emitUnionDeclaration: UnionDeclaration => Unit = {
    val default: TypeScriptDeclarationMapper.Resolved = { (decl, o) =>
      decl match {
        case UnionDeclaration(name, fields, _, superInterface) => {
          // Union interface
          o.print(s"export interface ${typeNaming(decl.reference)}")

          superInterface.foreach { iface =>
            o.print(s" extends ${typeNaming(iface.reference)}")
          }

          o.println(" {")

          // Abstract fields - common to all the subtypes
          list(fields).foreach(emitField(o, name, _))

          o.println("}")
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

          o.println("}")
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

          o.print(s"export interface ${typeNaming(decl.reference)}${typeParameters(typeParams)}")

          superInterface.foreach { iface =>
            o.print(s" extends ${typeNaming(iface.reference)}")
          }

          o.println(" {")

          list(fields).reverse.foreach(emitField(o, n, _))

          o.println("}")
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

      o.println(s"""export type ${tpeName} = ${vs mkString " | "}""")
      o.println()
      o.print(s"export const ${tpeName}Values = ")
      o.println(vs.mkString("[ ", ", ", " ]"))
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
    typeRef: TypeRef): String = {
    val tr = resolvedTypeMapper(settings, ownerType, member, _: TypeRef)

    typeRef match {
      case NumberRef => "number"

      case BooleanRef => "boolean"

      case StringRef => "string"

      case DateRef | DateTimeRef => "Date"

      case ArrayRef(innerType) =>
        s"ReadonlyArray<${tr(innerType)}>"

      case TupleRef(params) =>
        params.map(tr).mkString("[", ", ", "]")

      case custom @ CustomTypeRef(_, Nil) =>
        typeNaming(custom)

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

  private def withOut[T](
    decl: Declaration.Kind,
    name: String,
    imports: Set[TypeRef])(f: PrintStream => T): T = {
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

  type ImportResolver = Declaration => Option[Set[TypeRef]]

  /* See `TypeScriptPrinter` */
  type Printer = Function4[Settings, Declaration.Kind, String, Set[TypeRef], PrintStream]
}
