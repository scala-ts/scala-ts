package io.github.scalats.core

import scala.collection.immutable.ListSet

import io.github.scalats.typescript._

/**
 * Created by Milosz on 09.06.2016.
 */
final class Transpiler(config: Configuration) {
  // TODO: (low priority) Remove the transpiler phase?

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
      Member(
        scalaMember.name,
        transpileTypeRef(scalaMember.typeRef, inInterfaceContext = true))
    },
    typeParams = scalaClass.typeArgs,
    superInterface = superInterface)

  private def transpileClass(
    scalaClass: ScalaModel.CaseClass,
    superInterface: Option[InterfaceDeclaration]) = {
    ClassDeclaration(
      idToString(scalaClass.identifier),
      ClassConstructor(
        scalaClass.fields map { scalaMember =>
          ClassConstructorParameter(
            scalaMember.name,
            transpileTypeRef(scalaMember.typeRef, inInterfaceContext = false))
        }),
      values = scalaClass.values.map { v =>
        Member(v.name, transpileTypeRef(v.typeRef, false))
      },
      typeParams = scalaClass.typeArgs,
      superInterface)
  }

  private def transpileTypeRef(
    scalaTypeRef: ScalaModel.TypeRef,
    inInterfaceContext: Boolean): TypeRef = scalaTypeRef match {
    case ScalaModel.BigDecimalRef |
      ScalaModel.BigIntegerRef |
      ScalaModel.DoubleRef |
      ScalaModel.IntRef |
      ScalaModel.LongRef =>
      NumberRef

    case ScalaModel.BooleanRef =>
      BooleanRef

    case ScalaModel.StringRef | ScalaModel.UuidRef =>
      StringRef

    case ScalaModel.SeqRef(innerType) =>
      ArrayRef(transpileTypeRef(innerType, inInterfaceContext))

    case ScalaModel.EnumerationRef(id) =>
      SimpleTypeRef(idToString(id))

    case ScalaModel.TupleRef(typeArgs) =>
      TupleRef(
        typeArgs.map(transpileTypeRef(_, inInterfaceContext)))

    case ScalaModel.CaseClassRef(id, typeArgs) => {
      val name = {
        if (inInterfaceContext) toInterfaceName(id)
        else idToString(id)
      }

      CustomTypeRef(
        name, typeArgs.map(transpileTypeRef(_, inInterfaceContext)))
    }

    case ScalaModel.DateRef =>
      DateRef

    case ScalaModel.DateTimeRef =>
      DateTimeRef

    case ScalaModel.TypeParamRef(name) =>
      SimpleTypeRef(name)

    case ScalaModel.OptionRef(innerType) =>
      NullableType(
        transpileTypeRef(innerType, inInterfaceContext))

    case ScalaModel.MapRef(kT, vT) => MapType(
      transpileTypeRef(kT, inInterfaceContext),
      transpileTypeRef(vT, inInterfaceContext))

    case ScalaModel.UnionRef(possibilities) =>
      UnionType(possibilities.map { i =>
        transpileTypeRef(i, inInterfaceContext)
      })

    case ScalaModel.UnknownTypeRef(_) =>
      StringRef
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
