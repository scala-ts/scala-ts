package com.mpc.scalats.core

/**
  * Created by Milosz on 09.06.2016.
  */
object Compiler {

  def compile(scalaClasses: List[ScalaModel.CaseClass]): List[TypeScriptModel.InterfaceDeclaration] = {
    scalaClasses map { scalaClass =>
      TypeScriptModel.InterfaceDeclaration(
        scalaClass.name,
        scalaClass.members map { scalaMember =>
          TypeScriptModel.Member(
            scalaMember.name,
            compileTypeRef(scalaMember.typeRef)
          )
        }
      )
    }
  }

  def compileTypeRef(scalaTypeRef: ScalaModel.TypeRef): TypeScriptModel.TypeRef = scalaTypeRef match {
    case ScalaModel.IntRef =>
      TypeScriptModel.NumberRef
    case ScalaModel.StringRef =>
      TypeScriptModel.StringRef
    case ScalaModel.SeqRef(innerType) =>
      TypeScriptModel.ArrayRef(compileTypeRef(innerType))
    case ScalaModel.CaseClassRef(name, _) =>
      TypeScriptModel.InterfaceRef(name)
    case ScalaModel.DateRef =>
      TypeScriptModel.DateRef
    case ScalaModel.DateTimeRef =>
      TypeScriptModel.DateTimeRef
  }

}
