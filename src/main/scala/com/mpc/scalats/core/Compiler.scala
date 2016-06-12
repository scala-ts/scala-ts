package com.mpc.scalats.core

import com.mpc.scalats.core.TypeScriptModel.{ClassConstructor, ClassConstructorParameter}

/**
  * Created by Milosz on 09.06.2016.
  */
object Compiler {

  def compile(scalaClasses: List[ScalaModel.CaseClass]): List[TypeScriptModel.Declaration] = {
    scalaClasses flatMap { scalaClass =>
      val interfaceDecl = TypeScriptModel.InterfaceDeclaration(
        s"I${scalaClass.name}",
        scalaClass.members map { scalaMember =>
          TypeScriptModel.Member(
            scalaMember.name,
            compileTypeRef(scalaMember.typeRef)
          )
        }
      )
      val classDecl = TypeScriptModel.ClassDeclaration(
        scalaClass.name,
        ClassConstructor(
          scalaClass.members map { scalaMember =>
            ClassConstructorParameter(
              scalaMember.name,
              compileTypeRef(scalaMember.typeRef),
              Some(TypeScriptModel.AccessModifier.Public)
            )
          }
        )
      )
      List(interfaceDecl, classDecl)
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
      TypeScriptModel.InterfaceRef(s"I$name")
    case ScalaModel.DateRef =>
      TypeScriptModel.DateRef
    case ScalaModel.DateTimeRef =>
      TypeScriptModel.DateTimeRef
  }

}
