package com.mpc.scalats.core

import scala.collection.immutable.ListSet

import com.mpc.scalats.configuration.Config
import com.mpc.scalats.core.TypeScriptModel.{
  ClassConstructor,
  ClassConstructorParameter,
  NullRef,
  UndefinedRef,
  TypeParamRef,
  UnionDeclaration,
  SingletonDeclaration
}

/**
 * Created by Milosz on 09.06.2016.
 */
object Compiler {
  // TODO: Refactor as class with config parameter in ctor

  def compile(scalaTypes: ListSet[ScalaModel.TypeDef])(implicit config: Config): ListSet[TypeScriptModel.Declaration] = {
    scalaTypes.flatMap { typeDef =>
      typeDef match {
        case scalaClass: ScalaModel.CaseClass => {
          val clazz = {
            if (config.emitClasses) List(compileClass(scalaClass))
            else List.empty[TypeScriptModel.Declaration]
          }

          if (!config.emitInterfaces) clazz
          else compileInterface(scalaClass) :: clazz
        }

        case ScalaModel.CaseObject(name) =>
          List(SingletonDeclaration(name))

        case ScalaModel.SealedUnion(name, members) =>
          compile(members) + UnionDeclaration(
            name, members.map { m => TypeParamRef(m.name) })
      }
    }
  }

  // ---

  private def compileInterface(scalaClass: ScalaModel.CaseClass)(implicit config: Config) = {
    TypeScriptModel.InterfaceDeclaration(
      buildInterfaceName(scalaClass.name),
      scalaClass.members.map { scalaMember =>
        TypeScriptModel.Member(
          scalaMember.name,
          compileTypeRef(scalaMember.typeRef, inInterfaceContext = true)
        )
      },
      typeParams = scalaClass.params
    )
  }

  private def buildInterfaceName(name: String)(implicit config: Config) = {
    val prefix = if (config.prependIPrefix) "I" else ""
    s"${prefix}${name}"
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
  )(implicit config: Config): TypeScriptModel.TypeRef = scalaTypeRef match {
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
      val actualName =
        if (inInterfaceContext) buildInterfaceName(name) else name
      TypeScriptModel.CustomTypeRef(
        actualName,
        typeArgs.map(compileTypeRef(_, inInterfaceContext)))
    case ScalaModel.DateRef =>
      TypeScriptModel.DateRef
    case ScalaModel.DateTimeRef =>
      TypeScriptModel.DateTimeRef
    case ScalaModel.TypeParamRef(name) =>
      TypeScriptModel.TypeParamRef(name)

    case ScalaModel.OptionRef(innerType) if (
      config.optionToNullable && config.optionToUndefined) =>
      TypeScriptModel.UnionType(ListSet(
        TypeScriptModel.UnionType(ListSet(
          compileTypeRef(innerType, inInterfaceContext), NullRef)),
          UndefinedRef))

    case ScalaModel.OptionRef(innerType) if config.optionToNullable =>
      TypeScriptModel.UnionType(ListSet(
        compileTypeRef(innerType, inInterfaceContext),
        NullRef))

    case ScalaModel.MapRef(kT, vT) => TypeScriptModel.MapType(
      compileTypeRef(kT, inInterfaceContext),
      compileTypeRef(vT, inInterfaceContext))

    case ScalaModel.UnionRef(possibilities) =>
      TypeScriptModel.UnionType(possibilities.map { i =>
        compileTypeRef(i, inInterfaceContext)
      })

    case ScalaModel.OptionRef(innerType) if config.optionToUndefined =>
      TypeScriptModel.UnionType(ListSet(
        compileTypeRef(innerType, inInterfaceContext),
        UndefinedRef))

    case ScalaModel.UnknownTypeRef(_) =>
      TypeScriptModel.StringRef
  }
}
