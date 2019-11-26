package com.mpc.scalats.core

import com.mpc.scalats.configuration.Config
import com.mpc.scalats.core.TypeScriptModel.{ClassConstructor, ClassConstructorParameter, NullRef, UndefinedRef}

/**
  * Created by Milosz on 09.06.2016.
  */
object Compiler {

  def compile(scalaClasses: List[ScalaModel.CaseClass])(implicit config: Config): List[TypeScriptModel.Declaration] = {
    scalaClasses flatMap { scalaClass => List(compileInterface(scalaClass)) }
  }

  private def compileInterface(scalaClass: ScalaModel.CaseClass)(implicit config: Config) = {
    val prefix = config.interfacePrefix
    TypeScriptModel.InterfaceDeclaration(
      config.customNameMap.getOrElse(scalaClass.name,s"$prefix${scalaClass.name}"),
      scalaClass.members map { scalaMember =>
        TypeScriptModel.Member(
          scalaMember.name,
          compileTypeRef(scalaMember.typeRef)
        )
      },
      typeParams = scalaClass.params,
      parent = scalaClass.parent.map(p => s"$prefix$p")
    )
  }

  private def compileTypeRef(
    scalaTypeRef: ScalaModel.TypeRef
  )
    (implicit config: Config): TypeScriptModel.TypeRef = scalaTypeRef match {
    case ScalaModel.IntRef =>
      TypeScriptModel.NumberRef
    case ScalaModel.LongRef =>
      TypeScriptModel.NumberRef
    case ScalaModel.DoubleRef =>
      TypeScriptModel.NumberRef
    case ScalaModel.FloatRef =>
      TypeScriptModel.NumberRef
    case ScalaModel.BigDecimalRef =>
      TypeScriptModel.NumberRef
    case ScalaModel.BooleanRef =>
      TypeScriptModel.BooleanRef
    case ScalaModel.StringRef =>
      TypeScriptModel.StringRef
    case ScalaModel.SeqRef(innerType) =>
      TypeScriptModel.ArrayRef(compileTypeRef(innerType))
    case ScalaModel.CaseClassRef(name, typeArgs) =>
      val actualName = config.customNameMap.getOrElse(name,s"${config.interfacePrefix}$name")
      TypeScriptModel.CustomTypeRef(actualName, typeArgs.map(compileTypeRef(_)))
    case ScalaModel.DateRef =>
      TypeScriptModel.DateRef
    case ScalaModel.DateTimeRef =>
      TypeScriptModel.DateTimeRef
    case ScalaModel.TypeParamRef(name) =>
      TypeScriptModel.TypeParamRef(name)
    case ScalaModel.OptionRef(innerType) if config.optionToNullable && config.optionToUndefined =>
      TypeScriptModel.UnionType(TypeScriptModel.UnionType(compileTypeRef(innerType), NullRef), UndefinedRef)
    case ScalaModel.OptionRef(innerType) if config.optionToNullable =>
      TypeScriptModel.UnionType(compileTypeRef(innerType), NullRef)
    case ScalaModel.OptionRef(innerType) if config.optionToUndefined =>
      TypeScriptModel.UnionType(compileTypeRef(innerType), UndefinedRef)
    case ScalaModel.UnknownTypeRef(name) =>
       config.customNameMap.get(name).map(TypeScriptModel.UnknownTypeRef).getOrElse(
        TypeScriptModel.UnknownTypeRef(s"${config.interfacePrefix}$name")
      )
  }
}
