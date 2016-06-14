package com.mpc.scalats.core

import java.io.PrintStream

import com.mpc.scalats.core.TypeScriptModel.AccessModifier.{Private, Public}

object Emitter {

  import TypeScriptModel._

  def emit(declaration: List[Declaration], out: PrintStream): Unit = {
    declaration foreach {
      case InterfaceDeclaration(name, members) =>
        out.println(s"export interface $name {")
        members foreach { member =>
          out.println(s"\t${member.name}: ${emitTypeRef(member.typeRef)};")
        }
        out.println("}")
        out.println()
      case ClassDeclaration(name, ClassConstructor(parameters)) =>
        out.println(s"export class $name {")
        out.println(s"\tconstructor(")
        parameters.zipWithIndex foreach { case (parameter, index) =>
          val accessModifier = parameter.accessModifier match {
            case Some(Public) => "public "
            case Some(Private) => "private "
            case None => ""
          }
          out.print(s"\t\t$accessModifier${parameter.name}: ${emitTypeRef(parameter.typeRef)}")
          val endLine = if (index + 1 < parameters.length) "," else ""
          out.println(endLine)
        }
        out.println("\t) {}")
        out.println("}")
    }
  }

  def emitTypeRef(typeRef: TypeRef): String = typeRef match {
    case NumberRef => "number"
    case BooleanRef => "boolean"
    case StringRef | DateRef | DateTimeRef => "string"
    case ArrayRef(innerType) => s"${emitTypeRef(innerType)}[]"
    case CustomTypeRef(name) => name
    case UnknownTypeRef(typeName) => typeName
  }

}
