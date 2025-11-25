package io.github.scalats.core

import io.github.scalats.{ scala => ScalaModel, UtilCompat }
import io.github.scalats.ast._

import Internals.ListSet

/**
 * Created by Milosz on 09.06.2016.
 */
final class Transpiler(config: Settings, logger: Logger) {
  // TODO: (low priority) Remove the transpiler phase?

  @inline def apply(
      scalaTypes: Map[String, ListSet[ScalaModel.TypeDef]]
    ): Map[String, ListSet[Declaration]] =
    apply(scalaTypes, superInterface = None)

  def apply(
      scalaTypes: Map[String, ListSet[ScalaModel.TypeDef]],
      superInterface: Option[InterfaceDeclaration]
    ): Map[String, ListSet[Declaration]] = scalaTypes.headOption match {
    case Some((name, tps)) if tps.nonEmpty =>
      transpile(
        name,
        superInterface,
        tps,
        UtilCompat.mapValues(scalaTypes.tail)(superInterface -> _).toMap,
        ListSet(),
        Map.empty
      )

    case _ =>
      Map.empty
  }

  private def transpile(
      name: String,
      superInterface: Option[InterfaceDeclaration],
      tps: ListSet[ScalaModel.TypeDef],
      scalaTypes: Map[
        String,
        (Option[InterfaceDeclaration], ListSet[ScalaModel.TypeDef])
      ],
      decls: ListSet[Declaration],
      out: Map[String, ListSet[Declaration]]
    ): Map[String, ListSet[Declaration]] = tps.headOption match {
    case Some(scalaClass: ScalaModel.CaseClass) =>
      transpile(
        name,
        superInterface,
        tps.tail,
        scalaTypes,
        decls + transpileInterface(scalaClass, superInterface),
        out
      )

    case Some(valueClass: ScalaModel.ValueClass) =>
      transpile(
        name,
        superInterface,
        tps.tail,
        scalaTypes,
        decls + transpileValueClass(valueClass),
        out
      )

    case Some(ScalaModel.EnumerationDef(id, possibilities, vs)) => {
      val values = vs.map(transpileTypeInvariant)

      transpile(
        name,
        superInterface,
        tps.tail,
        scalaTypes,
        decls + EnumDeclaration(idToString(id), possibilities, values),
        out
      )
    }

    case Some(ScalaModel.CaseObject(id, members)) => {
      val values = members.filter {
        case ScalaModel.ObjectInvariant(
              _,
              ScalaModel.CaseObjectRef(oid)
            ) =>
          scalaTypes.get(oid.name).exists {
            case (_, rtps) =>
              rtps.exists {
                case ScalaModel.CaseObject(_, vs) =>
                  // Not empty or a companion only object
                  vs.nonEmpty || rtps.tail.isEmpty

                case _ => false
              }
          }

        case _ =>
          true
      }.map(transpileTypeInvariant)

      transpile(
        name,
        superInterface,
        tps.tail,
        scalaTypes,
        decls + SingletonDeclaration(idToString(id), values, superInterface),
        out
      )
    }

    case Some(ScalaModel.SealedUnion(id, fields, possibilities)) => {
      // TODO: Make sure scala type for referenced as possibilities there are not transpiled before, otherwise, the superInterface won't be properly set for them

      val ifaceFields = fields.map { scalaMember =>
        Member(
          scalaMember.name,
          transpileTypeRef(scalaMember.typeRef, false)
        )
      }

      val unionRef = InterfaceDeclaration(
        idToString(id),
        ifaceFields,
        List.empty[String],
        superInterface,
        union = true
      )

      val updTypes = possibilities
        .filterNot(_.identifier.name == name)
        .foldLeft(scalaTypes) { (acc, p) =>
          val pn = idToString(p.identifier)

          if (acc contains pn) {
            acc.transform {
              case (`pn`, ((s @ Some(`unionRef`)), ts)) =>
                s -> (ts + p)

              case (`pn`, (None, ts)) =>
                Some(unionRef) -> (ts + p)

              case (`pn`, ((s @ Some(_)), ts)) => {
                logger.warning(s"Unexpected super interface: $s != $unionRef")

                s -> ts
              }

              case (_, (s, ts)) =>
                s -> ts
            }
          } else {
            acc + (pn -> (Some(unionRef) -> ListSet(p)))
          }
        }

      transpile(
        name,
        superInterface,
        tps.tail,
        updTypes,
        decls + UnionDeclaration(
          idToString(id),
          ifaceFields,
          possibilities.map {
            case ScalaModel.CaseObject(pid, values) =>
              SingletonTypeRef(
                name = idToString(pid),
                values = values.map(transpileTypeInvariant)
              )

            case ScalaModel.CaseClass(pid, _, _, tpeArgs) =>
              CustomTypeRef(
                idToString(pid),
                tpeArgs.map { SimpleTypeRef(_) }
              )

            case m =>
              CustomTypeRef(idToString(m.identifier), List.empty)
          },
          superInterface
        ),
        out
      )
    }

    case None => {
      val newOut = out.get(name) match {
        case Some(ds) =>
          out.updated(name, ds ++ decls)

        case None =>
          out + (name -> decls)
      }

      scalaTypes.headOption match {
        case Some((newName, (newSuper, newTps))) if newTps.nonEmpty => {
          transpile(
            newName,
            newSuper,
            newTps,
            scalaTypes.tail,
            ListSet(),
            newOut
          )
        }

        case _ =>
          newOut
      }
    }
  }

  private def transpileSimpleInvariant(
      invariant: ScalaModel.TypeInvariant
    ): Value.Simple = invariant match {
    case lit: ScalaModel.LiteralInvariant =>
      LiteralValue(
        name = lit.name,
        typeRef = transpileTypeRef(lit.typeRef, false),
        rawValue = lit.value
      )

    case sel: ScalaModel.SelectInvariant =>
      SelectValue(
        name = sel.name,
        typeRef = transpileTypeRef(sel.typeRef, false),
        qualifier = transpileTypeRef(sel.qualifier, false),
        term = sel.term
      )

    case _ =>
      ??? // TODO
  }

  private def transpileTypeInvariant(
      invariant: ScalaModel.TypeInvariant
    ): Value = invariant match {
    case tpl: ScalaModel.TupleInvariant =>
      TupleValue(
        name = tpl.name,
        typeRef = transpileTypeRef(tpl.typeRef, false),
        values = tpl.values.map(transpileTypeInvariant)
      )

    case list: ScalaModel.ListInvariant =>
      ListValue(
        name = list.name,
        typeRef = transpileTypeRef(list.typeRef, false),
        valueTypeRef = transpileTypeRef(list.valueTypeRef, false),
        elements = list.values.map(transpileTypeInvariant)
      )

    case set: ScalaModel.SetInvariant =>
      SetValue(
        name = set.name,
        typeRef = transpileTypeRef(set.typeRef, false),
        valueTypeRef = transpileTypeRef(set.valueTypeRef, false),
        elements = set.values.map(transpileTypeInvariant)
      )

    case dict: ScalaModel.DictionaryInvariant =>
      DictionaryValue(
        name = dict.name,
        keyTypeRef = transpileTypeRef(dict.keyTypeRef, false),
        valueTypeRef = transpileTypeRef(dict.valueTypeRef, false),
        entries = dict.entries.map {
          case (k, v) =>
            transpileSimpleInvariant(k) -> transpileTypeInvariant(v)
        }
      )

    case ScalaModel.MergedListsInvariant(name, valueTpe, children) =>
      MergedListsValue(
        name = name,
        valueTypeRef = transpileTypeRef(valueTpe, false),
        children = children.map(transpileTypeInvariant)
      )

    case ScalaModel.MergedSetsInvariant(name, valueTpe, children) =>
      MergedSetsValue(
        name = name,
        valueTypeRef = transpileTypeRef(valueTpe, false),
        children = children.map(transpileTypeInvariant)
      )

    case ScalaModel.ObjectInvariant(name, objTpe) =>
      SingletonValue(name = name, typeRef = transpileTypeRef(objTpe, false))

    case _ =>
      transpileSimpleInvariant(invariant)
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
    case ScalaModel.BigDecimalRef =>
      NumberRef.bigDecimal

    case ScalaModel.BigIntegerRef =>
      NumberRef.bigInt

    case ScalaModel.DoubleRef =>
      NumberRef.double

    case ScalaModel.IntRef =>
      NumberRef.int

    case ScalaModel.LongRef =>
      NumberRef.long

    case ScalaModel.BooleanRef =>
      BooleanRef

    case ScalaModel.StringRef | ScalaModel.UuidRef =>
      StringRef

    case ScalaModel.ThisTypeRef =>
      ThisTypeRef

    case ScalaModel.TaggedRef(id, tagged) =>
      TaggedRef(idToString(id), transpileTypeRef(tagged, false))

    case ScalaModel.SetRef(innerType) =>
      SetRef(transpileTypeRef(innerType, inInterfaceContext))

    case ScalaModel.ListRef(innerType, nonEmpty) =>
      ArrayRef(transpileTypeRef(innerType, inInterfaceContext), nonEmpty)

    case ScalaModel.EnumerationRef(id) =>
      CustomTypeRef(idToString(id))

    case ScalaModel.TupleRef(typeArgs) =>
      TupleRef(typeArgs.map(transpileTypeRef(_, inInterfaceContext)))

    case ScalaModel.CaseClassRef(id, typeArgs) =>
      CustomTypeRef(
        idToString(id),
        typeArgs.map(transpileTypeRef(_, inInterfaceContext))
      )

    case ScalaModel.CaseObjectRef(id) =>
      SingletonTypeRef(name = idToString(id), values = ListSet.empty)

    case ScalaModel.DateRef =>
      DateRef

    case ScalaModel.DateTimeRef =>
      DateTimeRef

    case ScalaModel.TimeRef =>
      TimeRef

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
