package org.scalats.core

import scala.collection.immutable.ListSet

import org.scalats.core.TypeScriptModel.{
  ClassConstructor,
  ClassConstructorParameter,
  CustomTypeRef,
  Declaration,
  EnumDeclaration,
  InterfaceDeclaration,
  Member,
  NullRef,
  UndefinedRef,
  UnionDeclaration,
  SimpleTypeRef,
  SingletonDeclaration
}

/**
 * Created by Milosz on 09.06.2016.
 */
final class Transpiler(config: Configuration) {
  @inline def apply(scalaTypes: ListSet[ScalaModel.TypeDef]): ListSet[Declaration] = apply(scalaTypes, superInterface = None)

  def apply(
    scalaTypes: ListSet[ScalaModel.TypeDef],
    superInterface: Option[InterfaceDeclaration]): ListSet[Declaration] =
    scalaTypes.flatMap { typeDef =>
      typeDef match {
        case scalaClass: ScalaModel.CaseClass => {
          val clazz = {
            if (config.emitClasses) {
              ListSet[Declaration](transpileClass(scalaClass, superInterface))
            } else ListSet.empty[Declaration]
          }

          if (!config.emitInterfaces) clazz
          else ListSet[Declaration](
            transpileInterface(scalaClass, superInterface)) ++ clazz
        }

        case ScalaModel.Enumeration(name, values) => {
          ListSet[Declaration](
            EnumDeclaration(name, values))
        }

        case ScalaModel.CaseObject(name, members) => {
          val values = members.map { scalaMember =>
            Member(
              scalaMember.name,
              transpileTypeRef(scalaMember.typeRef, false))
          }

          ListSet[Declaration](
            SingletonDeclaration(name, values, superInterface))
        }

        case ScalaModel.SealedUnion(name, fields, possibilities) => {
          val ifaceFields = fields.map { scalaMember =>
            Member(
              scalaMember.name,
              transpileTypeRef(scalaMember.typeRef, false))
          }

          val unionRef = InterfaceDeclaration(
            s"I${qualifiedIdentifierToString(name)}", ifaceFields, ListSet.empty[String], superInterface)

          apply(possibilities, Some(unionRef)) + UnionDeclaration(
            name,
            ifaceFields,
            possibilities.map {
              case ScalaModel.CaseObject(nme, _) =>
                CustomTypeRef(nme, ListSet.empty)

              case ScalaModel.CaseClass(n, _, _, tpeArgs) => {
                val nme = if (config.emitInterfaces) s"I${qualifiedIdentifierToString(n)}" else qualifiedIdentifierToString(n)
                CustomTypeRef(nme, tpeArgs.map { SimpleTypeRef(_) })
              }

              case m =>
                CustomTypeRef(buildInterfaceName(m.identifier), ListSet.empty)
            },
            superInterface)
        }
      }
    }

  private def transpileInterface(
    scalaClass: ScalaModel.CaseClass,
    superInterface: Option[InterfaceDeclaration]) = InterfaceDeclaration(
    buildInterfaceName(scalaClass.identifier),
    scalaClass.fields.map { scalaMember =>
      TypeScriptModel.Member(
        scalaMember.name,
        transpileTypeRef(scalaMember.typeRef, inInterfaceContext = true))
    },
    typeParams = scalaClass.typeArgs,
    superInterface = superInterface)

  private def buildInterfaceName(name: String) = {
    val prefix = if (config.prependIPrefix) "I" else ""
    s"${prefix}${name}"
  }

  private def transpileClass(
    scalaClass: ScalaModel.CaseClass,
    superInterface: Option[InterfaceDeclaration]) = {
    TypeScriptModel.ClassDeclaration(
      scalaClass.identifier,
      ClassConstructor(
        scalaClass.fields map { scalaMember =>
          ClassConstructorParameter(
            scalaMember.name,
            transpileTypeRef(scalaMember.typeRef, inInterfaceContext = false))
        }),
      values = scalaClass.values.map { v =>
        TypeScriptModel.Member(v.name, transpileTypeRef(v.typeRef, false))
      },
      typeParams = scalaClass.typeArgs,
      superInterface)
  }

  private def transpileTypeRef(
    scalaTypeRef: ScalaModel.TypeRef,
    inInterfaceContext: Boolean): TypeScriptModel.TypeRef = scalaTypeRef match {
    case ScalaModel.IntRef =>
      TypeScriptModel.NumberRef
    case ScalaModel.LongRef =>
      TypeScriptModel.NumberRef
    case ScalaModel.DoubleRef =>
      TypeScriptModel.NumberRef
    case ScalaModel.BooleanRef =>
      TypeScriptModel.BooleanRef
    case ScalaModel.StringRef | ScalaModel.UuidRef =>
      TypeScriptModel.StringRef

    case ScalaModel.SeqRef(innerType) =>
      TypeScriptModel.ArrayRef(transpileTypeRef(innerType, inInterfaceContext))
    case ScalaModel.EnumerationRef(name) =>
      TypeScriptModel.SimpleTypeRef(name)

    case ScalaModel.CaseClassRef(name, typeArgs) => {
      val actualName = if (inInterfaceContext) buildInterfaceName(name) else qualifiedIdentifierToString(name)
      TypeScriptModel.CustomTypeRef(
        actualName, typeArgs.map(transpileTypeRef(_, inInterfaceContext)))
    }

    case ScalaModel.DateRef =>
      TypeScriptModel.DateRef
    case ScalaModel.DateTimeRef =>
      TypeScriptModel.DateTimeRef

    case ScalaModel.TypeParamRef(name) =>
      TypeScriptModel.SimpleTypeRef(name)

    case ScalaModel.OptionRef(innerType) if (
      config.optionToNullable && config.optionToUndefined) =>
      TypeScriptModel.UnionType(ListSet(
        TypeScriptModel.UnionType(ListSet(
          transpileTypeRef(innerType, inInterfaceContext), NullRef)),
        UndefinedRef))

    case ScalaModel.OptionRef(innerType) if config.optionToNullable =>
      TypeScriptModel.UnionType(ListSet(
        transpileTypeRef(innerType, inInterfaceContext),
        NullRef))

    case ScalaModel.MapRef(kT, vT) => TypeScriptModel.MapType(
      transpileTypeRef(kT, inInterfaceContext),
      transpileTypeRef(vT, inInterfaceContext))

    case ScalaModel.UnionRef(possibilities) =>
      TypeScriptModel.UnionType(possibilities.map { i =>
        transpileTypeRef(i, inInterfaceContext)
      })

    case ScalaModel.OptionRef(innerType) if config.optionToUndefined =>
      TypeScriptModel.UnionType(ListSet(
        transpileTypeRef(innerType, inInterfaceContext),
        UndefinedRef))

    case ScalaModel.UnknownTypeRef(_) =>
      TypeScriptModel.StringRef
  }
}
