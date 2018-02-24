package com.mpc.scalats.core

import com.mpc.scalats.configuration.Config
import com.mpc.scalats.core.ScalaModel.CaseClassRef
import com.mpc.scalats.core.TypeScriptModel.{ClassConstructor, ClassConstructorParameter, NullRef, UndefinedRef}

/**
  * Created by Milosz on 09.06.2016.
  */
object Compiler {

  def compile(scalaClasses: List[ScalaModel.Entity])(implicit config: Config): List[TypeScriptModel.Declaration] = {
    scalaClasses flatMap { scalaClass =>
      val interface = if (config.emitInterfaces) List(compileInterface(scalaClass)) else List.empty
      val clazz = if (config.emitClasses) List(compileClass(scalaClass)) else List.empty
      interface ++ clazz
    }
  }

  private def compileInterface(scalaClass: ScalaModel.Entity)(implicit config: Config) = {
    TypeScriptModel.InterfaceDeclaration(
      s"I${scalaClass.name}",
      scalaClass.members.map { scalaMember =>
        TypeScriptModel.Member(
          scalaMember.name,
          compileTypeRef(scalaMember.typeRef, inInterfaceContext = true)
        )
      }.toSet,
      typeParams = scalaClass.params
    )
  }

  private def compileClass(scalaClass: ScalaModel.Entity)(implicit config: Config) = {
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
      typeParams = scalaClass.params,
      baseClasses = scalaClass.baseTypes
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
      val actualName = if (inInterfaceContext) s"I$name" else name
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
    case ScalaModel.MapRef(kT, vT) =>
      TypeScriptModel.MapType(compileTypeRef(kT, inInterfaceContext), compileTypeRef(vT, inInterfaceContext))
    case ScalaModel.UnionRef(i, i2) =>
      TypeScriptModel.UnionType(compileTypeRef(i, inInterfaceContext), compileTypeRef(i2, inInterfaceContext))
    case ScalaModel.OptionRef(innerType) if config.optionToUndefined =>
      TypeScriptModel.UnionType(compileTypeRef(innerType, inInterfaceContext), UndefinedRef)
    case ScalaModel.UnknownTypeRef(_) =>
      TypeScriptModel.StringRef
  }

}
