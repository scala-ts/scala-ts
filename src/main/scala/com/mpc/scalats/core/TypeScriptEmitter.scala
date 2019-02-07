package com.mpc.scalats.core

import java.io.PrintStream

import com.mpc.scalats.core.TypeScriptModel.AccessModifier.{Private, Public}
import com.mpc.scalats.core.TypeScriptModel.{DateRef, DateTimeRef}

object TypeScriptEmitter {

  import TypeScriptModel._

  def emit(declaration: List[Declaration], out: PrintStream): Unit = {
    declaration foreach {
      case decl: InterfaceDeclaration =>
        emitInterfaceDeclaration(decl, out)
    }
    out.flush()
    out.close()
  }

  private def emitInterfaceDeclaration(decl: InterfaceDeclaration, out: PrintStream): Unit = {
    val InterfaceDeclaration(name, members, typeParams, parent) = decl
    out.print(s"export interface $name")
    emitTypeParams(decl.typeParams, out)
    parent.foreach(p => out.print(s" extends $p"))
    out.println(" {")
    members foreach { member =>
      out.println(s"\t${member.name}: ${getTypeRefString(member.typeRef)}")
    }
    out.println("}")
    out.println()
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
    case UnionType(inner1, inner2) => s"(${getTypeRefString(inner1)} | ${getTypeRefString(inner2)})"
    case NullRef => "null"
    case UndefinedRef => "undefined"
  }

}
