package com.mpc.scalats.core

import java.io.PrintStream

import scala.collection.immutable.ListSet

import com.mpc.scalats.core.TypeScriptModel.AccessModifier.{ Private, Public }

// TODO: Emit Option (space-lift?)
// TODO: Use a template engine (velocity?)
final class TypeScriptEmitter(indent: String) {

  import TypeScriptModel._

  def emit(declaration: ListSet[Declaration], out: PrintStream): Unit = {
    declaration.foreach { d =>
      d match {
        case decl: InterfaceDeclaration =>
          emitInterfaceDeclaration(decl, out)

        case decl: ClassDeclaration =>
          emitClassDeclaration(decl, out)

        case SingletonDeclaration(name) =>
          emitSingletonDeclaration(name, out)

        case UnionDeclaration(name, possibilities) =>
          emitUnionDeclaration(name, possibilities, out)
      }

      println()
    }
  }

  // ---

  private def emitUnionDeclaration(
    name: String,
    possibilities: ListSet[UnknownTypeRef],
    out: PrintStream): Unit = {
    out.println(s"""export type $name = ${possibilities.map(_.name) mkString " | "};""")
  }

  private def emitSingletonDeclaration(name: String, out: PrintStream): Unit = {
    // Class definition
    out.println(s"export class $name {")
    out.println(s"${indent}private static instance: $name;\n")

    out.println(s"${indent}private constructor() {}\n")
    out.println(s"${indent}public static getInstance() {")
    out.println(s"${indent}${indent}if (!${name}.instance) {")
    out.println(s"${indent}${indent}${indent}${name}.instance = new ${name}();")
    out.println(s"${indent}${indent}}\n")
    out.println(s"${indent}${indent}return ${name}.instance;")
    out.println(s"${indent}}")

    out.println("}")
  }

  private def emitInterfaceDeclaration(
    decl: InterfaceDeclaration,
    out: PrintStream): Unit = {

    val InterfaceDeclaration(name, members, typeParams) = decl
    out.print(s"export interface $name")
    emitTypeParams(typeParams, out)
    out.println(" {")
    members.foreach { member =>
      out.println(s"${indent}${member.name}: ${getTypeRefString(member.typeRef)};")
    }
    out.println("}")
  }

  private def emitClassDeclaration(
    decl: ClassDeclaration,
    out: PrintStream): Unit = {

    val ClassDeclaration(name, ClassConstructor(parameters), typeParams) = decl

    // Class definition
    out.print(s"export class $name")
    emitTypeParams(typeParams, out)
    out.println(" {")

    // Class fields
    parameters.foreach { parameter =>
      out.print(indent)

      parameter.accessModifier.foreach {
        case Public => out.print("public ")
        case Private => out.print("private ")
      }

      out.println(s"${parameter.name}: ${getTypeRefString(parameter.typeRef)};")
    }

    // Class constructor
    out.println(s"\n${indent}constructor(")

    parameters.zipWithIndex.foreach {
      case (parameter, index) =>
        out.print(s"${indent}${indent}${parameter.name}: ${getTypeRefString(parameter.typeRef)}")
        val endLine = if (index + 1 < parameters.length) "," else ""
        out.println(endLine)
    }

    out.println(s"${indent}) {")

    parameters.foreach { parameter =>
      out.println(s"${indent}${indent}this.${parameter.name} = ${parameter.name};")
    }
    out.println(s"${indent}}")

    out.println("}")
  }

  private def emitTypeParams(params: List[String], out: PrintStream) =
    if (params.nonEmpty) {
      out.print("<")
      out.print(params.mkString(", "))
      out.print(">")
    }

  private def getTypeRefString(typeRef: TypeRef): String = typeRef match {
    case NumberRef => "number"
    case BooleanRef => "boolean"
    case StringRef => "string"
    case DateRef | DateTimeRef => "Date"
    case ArrayRef(innerType) => s"${getTypeRefString(innerType)}[]"
    case CustomTypeRef(name, params) if params.isEmpty => name
    case CustomTypeRef(name, params) if params.nonEmpty =>
      s"$name<${params.map(getTypeRefString).mkString(", ")}>"
    case UnknownTypeRef(typeName) => typeName
    case TypeParamRef(param) => param

    case UnionType(possibilities) =>
      possibilities.map(getTypeRefString).mkString("(", " | ", ")")

    case MapType(keyType, valueType) => s"{ [key: ${getTypeRefString(keyType)}]: ${getTypeRefString(valueType)} }"

    case NullRef => "null"
    case UndefinedRef => "undefined"
  }

}
