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
  typeMapper: TypeScriptEmitter.TypeMapper) {

  import Internals.list

  import settings.{ typescriptIndent => indent }
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

  private def emitUnionDeclaration(
    decl: UnionDeclaration): Unit = {

    val UnionDeclaration(name, fields, _, superInterface) = decl
    import decl.requires

    withOut(Declaration.Union, name, requires) { o =>
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
  }

  private def emitField(o: PrintStream, name: String, member: Member): Unit = {
    val tsField = settings.fieldMapper(
      settings, name, member.name, member.typeRef)

    val nameSuffix: String = {
      if (tsField.flags contains TypeScriptField.omitable) "?"
      else ""
    }

    o.println(s"${indent}${tsField.name}${nameSuffix}: ${resolvedTypeMapper(name, tsField, member.typeRef)}${lineSeparator}")
  }

  private def emitSingletonDeclaration(decl: SingletonDeclaration): Unit = {
    val SingletonDeclaration(name, members, superInterface) = decl
    import decl.requires

    withOut(Declaration.Singleton, name, requires) { o =>
      if (members.nonEmpty) {
        def mkString = members.map {
          case Member(nme, tpe) => s"$nme ($tpe)"
        }.mkString(", ")

        o.println(s"// WARNING: Cannot emit static members for properties of singleton '$name': ${mkString}")
      }

      val tpeName = typeNaming(decl.reference)

      // Class definition
      o.print(s"export class ${tpeName}")

      superInterface.filter(_ => members.isEmpty).foreach { iface =>
        o.print(s" implements ${typeNaming(iface.reference)}")
      }

      o.println(" {")

      o.println(s"${indent}private static instance: $tpeName${lineSeparator}\n")

      o.println(s"${indent}private constructor() {}\n")
      o.println(s"${indent}public static getInstance() {")
      o.println(s"${indent}${indent}if (!${tpeName}.instance) {")
      o.println(s"${indent}${indent}${indent}${tpeName}.instance = new ${tpeName}()${lineSeparator}")
      o.println(s"${indent}${indent}}\n")
      o.println(s"${indent}${indent}return ${tpeName}.instance${lineSeparator}")
      o.println(s"${indent}}")

      /* TODO: (medium priority)
      if (settings.emitCodecs.enabled) {
        // Decoder factory: MyClass.fromData({..})
        o.println(s"\n${indent}public static fromData(data: any): ${name} {")
        o.println(s"${indent}${indent}return ${name}.instance${lineSeparator}")
        o.println(s"${indent}}")

        // Encoder
        o.println(s"\n${indent}public static toData(instance: ${name}): any {")
        o.println(s"${indent}${indent}return instance${lineSeparator}")
        o.println(s"${indent}}")
      }
       */

      o.println("}")
    }
  }

  private def emitInterfaceDeclaration(
    decl: InterfaceDeclaration): Unit = {
    val InterfaceDeclaration(name, fields, typeParams, superInterface) = decl

    withOut(Declaration.Interface, name, decl.requires) { o =>
      o.print(s"export interface ${typeNaming(decl.reference)}${typeParameters(typeParams)}")

      superInterface.foreach { iface =>
        o.print(s" extends ${typeNaming(iface.reference)}")
      }

      o.println(" {")

      list(fields).reverse.foreach(emitField(o, name, _))

      o.println("}")
    }
  }

  private def emitEnumDeclaration(decl: EnumDeclaration): Unit = {
    val EnumDeclaration(name, values) = decl

    withOut(Declaration.Enum, name, decl.requires) { o =>
      /* TODO: extension
      o.println(s"export enum ${typeNaming(decl.reference)} {")

      list(values).zipWithIndex.foreach {
        case (value, idx) =>
          if (idx > 0) {
            o.println(",")
          }

          o.print(s"${indent}${value} = '${value}'")
      }

      o.println()
      o.println("}")
       */

      val tpeName = typeNaming(decl.reference)
      val vs = list(values).map(v => s"'${v}'")

      o.println(s"""export type ${tpeName} = ${vs mkString " | "}""")
      o.println()
      o.println(
        s"""export const ${tpeName}Values = ${vs.mkString("[ ", ", ", " ]")}""")
    }
  }

  // ---

  @inline private def typeParameters(params: List[String]): String =
    if (params.isEmpty) "" else params.mkString("<", ", ", ">")

  private lazy val resolvedTypeMapper: TypeScriptTypeMapper.Resolved = {
    (ownerType: String, member: TypeScriptField, typeRef: TypeRef) =>
      typeMapper(resolvedTypeMapper, ownerType, member, typeRef).
        getOrElse(defaultTypeMapping(ownerType, member, typeRef))
  }

  private def defaultTypeMapping(
    ownerType: String,
    member: TypeScriptField,
    typeRef: TypeRef): String = {
    val tr = resolvedTypeMapper(ownerType, member, _: TypeRef)

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
    requires: Set[TypeRef])(f: PrintStream => T): T = {
    lazy val print = out(settings, decl, name, requires)

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

private[core] object TypeScriptEmitter {
  type TypeMapper = Function4[TypeScriptTypeMapper.Resolved, String, TypeScriptField, TypeRef, Option[String]]

  /* See `TypeScriptPrinter` */
  type Printer = Function4[Settings, Declaration.Kind, String, Set[TypeRef], PrintStream]
}
