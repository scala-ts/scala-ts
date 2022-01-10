package io.github.scalats.core

import scala.collection.immutable.ListSet

import io.github.scalats.typescript._

/**
 * Created by Milosz on 09.06.2016.
 */
final class Transpiler(config: Settings) {
  // TODO: (low priority) Remove the transpiler phase?

  @inline def apply(
      scalaTypes: ListSet[ScalaModel.TypeDef]
    ): ListSet[Declaration] = apply(scalaTypes, superInterface = None)

  def apply(
      scalaTypes: ListSet[ScalaModel.TypeDef],
      superInterface: Option[InterfaceDeclaration]
    ): ListSet[Declaration] =
    scalaTypes.flatMap {
      case scalaClass: ScalaModel.CaseClass =>
        ListSet[Declaration](transpileInterface(scalaClass, superInterface))

      case valueClass: ScalaModel.ValueClass =>
        ListSet[Declaration](transpileValueClass(valueClass))

      case ScalaModel.EnumerationDef(id, values) =>
        ListSet[Declaration](EnumDeclaration(idToString(id), values))

      case ScalaModel.CaseObject(id, members) => {
        val values = members.map { scalaMember =>
          Value(
            scalaMember.name,
            transpileTypeRef(scalaMember.typeRef, false),
            scalaMember.value
          )
        }

        ListSet[Declaration](
          SingletonDeclaration(idToString(id), values, superInterface)
        )
      }

      case ScalaModel.SealedUnion(id, fields, possibilities) => {
        val ifaceFields = fields.map { scalaMember =>
          Member(scalaMember.name, transpileTypeRef(scalaMember.typeRef, false))
        }

        val unionRef = InterfaceDeclaration(
          idToString(id),
          ifaceFields,
          List.empty[String],
          superInterface,
          union = true
        )

        apply(possibilities, Some(unionRef)) + UnionDeclaration(
          idToString(id),
          ifaceFields,
          possibilities.map {
            case ScalaModel.CaseObject(pid, values) =>
              SingletonTypeRef(
                name = idToString(pid),
                values = values.map { v =>
                  Value(
                    name = v.name,
                    typeRef = transpileTypeRef(v.typeRef, false),
                    rawValue = v.value
                  )
                }
              )

            case ScalaModel.CaseClass(pid, _, _, tpeArgs) =>
              CustomTypeRef(idToString(pid), tpeArgs.map { SimpleTypeRef(_) })

            case m =>
              CustomTypeRef(idToString(m.identifier), List.empty)
          },
          superInterface
        )
      }
    }

  private def transpileValueClass(valueClass: ScalaModel.ValueClass) =
    TaggedDeclaration(
      name = idToString(valueClass.identifier),
      field = Member(
        valueClass.field.name,
        transpileTypeRef(valueClass.field.typeRef, inInterfaceContext = false)
      )
    )

  private def transpileInterface(
      scalaClass: ScalaModel.CaseClass,
      superInterface: Option[InterfaceDeclaration]
    ) = {
    // TODO: (medium priority) Transpile values? (see former transpileClass)
    InterfaceDeclaration(
      idToString(scalaClass.identifier),
      scalaClass.fields.map { scalaMember =>
        Member(
          scalaMember.name,
          transpileTypeRef(scalaMember.typeRef, inInterfaceContext = true)
        )
      },
      typeParams = scalaClass.typeArgs,
      superInterface = superInterface,
      union = false
    )
  }

  private def transpileTypeRef(
      scalaTypeRef: ScalaModel.TypeRef,
      inInterfaceContext: Boolean
    ): TypeRef = scalaTypeRef match {
    case ScalaModel.BigDecimalRef | ScalaModel.BigIntegerRef |
        ScalaModel.DoubleRef | ScalaModel.IntRef | ScalaModel.LongRef =>
      NumberRef

    case ScalaModel.BooleanRef =>
      BooleanRef

    case ScalaModel.StringRef | ScalaModel.UuidRef =>
      StringRef

    case ScalaModel.TaggedRef(id, tagged) =>
      TaggedRef(idToString(id), transpileTypeRef(tagged, false))

    case ScalaModel.CollectionRef(innerType) =>
      ArrayRef(transpileTypeRef(innerType, inInterfaceContext))

    case ScalaModel.EnumerationRef(id) =>
      CustomTypeRef(idToString(id))

    case ScalaModel.TupleRef(typeArgs) =>
      TupleRef(typeArgs.map(transpileTypeRef(_, inInterfaceContext)))

    case ScalaModel.CaseClassRef(id, typeArgs) => {
      val name = {
        if (inInterfaceContext) idToString(id)
        else idToString(id)
      }

      CustomTypeRef(name, typeArgs.map(transpileTypeRef(_, inInterfaceContext)))
    }

    case ScalaModel.DateRef =>
      DateRef

    case ScalaModel.DateTimeRef =>
      DateTimeRef

    case ScalaModel.TypeParamRef(name) =>
      SimpleTypeRef(name)

    case ScalaModel.OptionRef(innerType) =>
      NullableType(transpileTypeRef(innerType, inInterfaceContext))

    case ScalaModel.MapRef(kT, vT) =>
      MapType(
        transpileTypeRef(kT, inInterfaceContext),
        transpileTypeRef(vT, inInterfaceContext)
      )

    case ScalaModel.UnionRef(possibilities) =>
      UnionType(possibilities.map { i =>
        transpileTypeRef(i, inInterfaceContext)
      })

    case ScalaModel.UnknownTypeRef(id) =>
      CustomTypeRef(idToString(id), List.empty)
  }

  private def idToString(identifier: ScalaModel.QualifiedIdentifier): String = {
    if (config.prependEnclosingClassNames) {
      s"${identifier.enclosingClassNames.mkString}${identifier.name}"
    } else {
      identifier.name
    }
  }
}
