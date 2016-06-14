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
            compileTypeRef(scalaMember.typeRef) match {
              case TypeScriptModel.CustomTypeRef(name) => TypeScriptModel.CustomTypeRef(s"I$name")
              case typeRef => typeRef
            }
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
    case ScalaModel.DoubleRef =>
      TypeScriptModel.NumberRef
    case ScalaModel.BooleanRef =>
      TypeScriptModel.BooleanRef
    case ScalaModel.StringRef =>
      TypeScriptModel.StringRef
    case ScalaModel.SeqRef(innerType) =>
      TypeScriptModel.ArrayRef(compileTypeRef(innerType))
    case ScalaModel.CaseClassRef(name, _) =>
      TypeScriptModel.CustomTypeRef(name)
    case ScalaModel.DateRef =>
      TypeScriptModel.DateRef
    case ScalaModel.DateTimeRef =>
      TypeScriptModel.DateTimeRef
    case ScalaModel.OptionRef(innerType) =>
      compileTypeRef(innerType) // TODO
    case ScalaModel.UnknownTypeRef(_, _) =>
      TypeScriptModel.StringRef

  }

}
