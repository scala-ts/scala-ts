package com.mpc.scalats.core

import com.mpc.scalats.configuration.Config
import com.mpc.scalats.core.TypeScriptModel.{ClassConstructor, ClassConstructorParameter, NullRef, UndefinedRef}

/**
  * Created by Milosz on 09.06.2016.
  */
object Compiler {

  def compile(scalaClasses: List[ScalaModel.CaseClass])(implicit config: Config): List[TypeScriptModel.Declaration] = {
    scalaClasses flatMap { scalaClass =>
      val interface = if (config.emitInterfaces) List(compileInterface(scalaClass)) else List.empty
      val clazz = if (config.emitClasses) List(compileClass(scalaClass)) else List.empty
      interface ++ clazz
    }
  }

  private def compileInterface(scalaClass: ScalaModel.CaseClass)(implicit config: Config) = {
    TypeScriptModel.InterfaceDeclaration(
      s"IElium${scalaClass.name}",
      scalaClass.members map { scalaMember =>
        TypeScriptModel.Member(
          scalaMember.name,
          compileTypeRef(scalaMember.typeRef, inInterfaceContext = true)
        )
      },
      typeParams = scalaClass.params
    )
  }

  private def compileClass(scalaClass: ScalaModel.CaseClass)(implicit config: Config) = {
    TypeScriptModel.ClassDeclaration(
      scalaClass.name,
      ClassConstructor(
        scalaClass.members map { scalaMember =>
          ClassConstructorParameter(
            scalaMember.name,
            compileTypeRef(scalaMember.typeRef, inInterfaceContext = false),
            Some(TypeScriptModel.AccessModifier.Public)
          )
        }
      ),
      typeParams = scalaClass.params
    )
  }

  private def compileTypeRef(
                              scalaTypeRef: ScalaModel.TypeRef,
                              inInterfaceContext: Boolean
                            )
                            (implicit config: Config): TypeScriptModel.TypeRef = scalaTypeRef match {
    case ScalaModel.IntRef =>
      TypeScriptModel.NumberRef
    case ScalaModel.LongRef =>
      TypeScriptModel.NumberRef
    case ScalaModel.DoubleRef =>
      TypeScriptModel.NumberRef
    case ScalaModel.BooleanRef =>
      TypeScriptModel.BooleanRef
    case ScalaModel.StringRef =>
      TypeScriptModel.StringRef
    case ScalaModel.SeqRef(innerType) =>
      TypeScriptModel.ArrayRef(compileTypeRef(innerType, inInterfaceContext))
    case ScalaModel.CaseClassRef(name, typeArgs) =>
      val actualName = if (inInterfaceContext) s"IElium$name" else name
      TypeScriptModel.CustomTypeRef(actualName, typeArgs.map(compileTypeRef(_, inInterfaceContext)))
    case ScalaModel.DateRef =>
      TypeScriptModel.DateRef
    case ScalaModel.DateTimeRef =>
      TypeScriptModel.DateTimeRef
    case ScalaModel.TypeParamRef(name) =>
      TypeScriptModel.TypeParamRef(name)
    case ScalaModel.OptionRef(innerType) if config.optionToNullable && config.optionToUndefined =>
      TypeScriptModel.UnionType(TypeScriptModel.UnionType(compileTypeRef(innerType, inInterfaceContext), NullRef), UndefinedRef)
    case ScalaModel.OptionRef(innerType) if config.optionToNullable =>
      TypeScriptModel.UnionType(compileTypeRef(innerType, inInterfaceContext), NullRef)
    case ScalaModel.OptionRef(innerType) if config.optionToUndefined =>
      TypeScriptModel.UnionType(compileTypeRef(innerType, inInterfaceContext), UndefinedRef)
    case ScalaModel.UnknownTypeRef(_) =>
      TypeScriptModel.StringRef
  }

}
