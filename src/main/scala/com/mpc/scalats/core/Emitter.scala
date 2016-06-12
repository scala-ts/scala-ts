package com.mpc.scalats.core

import java.io.PrintStream

object Emitter {

  import TypeScriptModel._

  def emit(interfaces: List[InterfaceDeclaration], out: PrintStream): Unit = {
    interfaces foreach { interface =>
      out.println(s"export interface ${interface.name} {")
      interface.members foreach { member =>
        out.println(s"\t${member.name}: ${emitTypeRef(member.typeRef)};")
      }
      out.println("}")
      out.println()
    }
  }

  def emitTypeRef(typeRef: TypeRef): String = typeRef match {
    case NumberRef => "number"
    case StringRef | DateRef | DateTimeRef => "string"
    case ArrayRef(innerType) => s"${emitTypeRef(innerType)}[]"
    case InterfaceRef(name) => name
  }

}
