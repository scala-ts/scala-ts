package io.github.scalats.core

import scala.collection.immutable.ListSet

/**
 * Created by Milosz on 09.06.2016.
 */
final class Transpiler(config: Configuration) {
  import TypeScriptModel._

  @inline def apply(scalaTypes: ListSet[ScalaModel.TypeDef]): ListSet[Declaration] = apply(scalaTypes, superInterface = None)

  def apply(
    scalaTypes: ListSet[ScalaModel.TypeDef],
    superInterface: Option[InterfaceDeclaration]): ListSet[Declaration] =
    scalaTypes.flatMap {
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

      case ScalaModel.Enumeration(id, values) => {
        ListSet[Declaration](EnumDeclaration(idToString(id), values))
      }

      case ScalaModel.CaseObject(id, members) => {
        val values = members.map { scalaMember =>
          Member(
            scalaMember.name,
            transpileTypeRef(scalaMember.typeRef, false))
        }

        ListSet[Declaration](
          SingletonDeclaration(idToString(id), values, superInterface))
      }

      case ScalaModel.SealedUnion(id, fields, possibilities) => {
        val ifaceFields = fields.map { scalaMember =>
          Member(
            scalaMember.name,
            transpileTypeRef(scalaMember.typeRef, false))
        }

        val unionRef = InterfaceDeclaration(
          toInterfaceName(id),
          ifaceFields, List.empty[String], superInterface)

        apply(possibilities, Some(unionRef)) + UnionDeclaration(
          idToString(id),
          ifaceFields,
          possibilities.map {
            case ScalaModel.CaseObject(pid, _) =>
              CustomTypeRef(idToString(pid), List.empty)

            case ScalaModel.CaseClass(pid, _, _, tpeArgs) =>
              CustomTypeRef(
                toInterfaceName(pid), tpeArgs.map { SimpleTypeRef(_) })

            case m =>
              CustomTypeRef(toInterfaceName(m.identifier), List.empty)
          },
          superInterface)
      }
    }

  private def transpileInterface(
    scalaClass: ScalaModel.CaseClass,
    superInterface: Option[InterfaceDeclaration]) = InterfaceDeclaration(
    toInterfaceName(scalaClass.identifier),
    scalaClass.fields.map { scalaMember =>
      TypeScriptModel.Member(
        scalaMember.name,
        transpileTypeRef(scalaMember.typeRef, inInterfaceContext = true))
    },
    typeParams = scalaClass.typeArgs,
    superInterface = superInterface)

  private def transpileClass(
    scalaClass: ScalaModel.CaseClass,
    superInterface: Option[InterfaceDeclaration]) = {
    TypeScriptModel.ClassDeclaration(
      idToString(scalaClass.identifier),
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
    case ScalaModel.BigDecimalRef |
      ScalaModel.DoubleRef |
      ScalaModel.IntRef |
      ScalaModel.LongRef =>
      TypeScriptModel.NumberRef

    case ScalaModel.BooleanRef =>
      TypeScriptModel.BooleanRef

    case ScalaModel.StringRef | ScalaModel.UuidRef =>
      TypeScriptModel.StringRef

    case ScalaModel.SeqRef(innerType) =>
      TypeScriptModel.ArrayRef(transpileTypeRef(innerType, inInterfaceContext))

    case ScalaModel.EnumerationRef(id) =>
      TypeScriptModel.SimpleTypeRef(idToString(id))

    case ScalaModel.TupleRef(typeArgs) =>
      TypeScriptModel.TupleRef(
        typeArgs.map(transpileTypeRef(_, inInterfaceContext)))

    case ScalaModel.CaseClassRef(id, typeArgs) => {
      val name = {
        if (inInterfaceContext) toInterfaceName(id)
        else idToString(id)
      }

      TypeScriptModel.CustomTypeRef(
        name, typeArgs.map(transpileTypeRef(_, inInterfaceContext)))
    }

    case ScalaModel.DateRef =>
      TypeScriptModel.DateRef

    case ScalaModel.DateTimeRef =>
      TypeScriptModel.DateTimeRef

    case ScalaModel.TypeParamRef(name) =>
      TypeScriptModel.SimpleTypeRef(name)

    case ScalaModel.OptionRef(innerType) =>
      TypeScriptModel.NullableType(
        transpileTypeRef(innerType, inInterfaceContext))

    case ScalaModel.MapRef(kT, vT) => TypeScriptModel.MapType(
      transpileTypeRef(kT, inInterfaceContext),
      transpileTypeRef(vT, inInterfaceContext))

    case ScalaModel.UnionRef(possibilities) =>
      TypeScriptModel.UnionType(possibilities.map { i =>
        transpileTypeRef(i, inInterfaceContext)
      })

    case ScalaModel.UnknownTypeRef(_) =>
      TypeScriptModel.StringRef
  }

  private def toInterfaceName(id: ScalaModel.QualifiedIdentifier) = {
    val prefix = if (config.prependIPrefix) "I" else ""
    s"${prefix}${idToString(id)}"
  }

  private def idToString(identifier: ScalaModel.QualifiedIdentifier): String = {
    if (config.prependEnclosingClassNames) {
      s"${identifier.enclosingClassNames.mkString}${identifier.name}"
    } else {
      identifier.name
    }
  }
}
