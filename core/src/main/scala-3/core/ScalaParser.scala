package io.github.scalats.core

import scala.util.control.NonFatal

import scala.collection.Factory

import scala.collection.immutable.ListSet

import dotty.tools.dotc.core.Contexts.Context
import dotty.tools.dotc.core.{ Constants, Names, Denotations, Definitions }
import dotty.tools.dotc.core.Symbols, Symbols.Symbol
import dotty.tools.dotc.core.Types, Types.{ Type, MethodType }
import dotty.tools.dotc.core.Flags
import dotty.tools.dotc.util.NoSourcePosition
import dotty.tools.dotc.ast.tpd

import io.github.scalats.{ scala => ScalaModel }

final class ScalaParser(
    compiled: Set[String],
    logger: Logger
  )(using
    ctx: Context) {

  import tpd._
  import ScalaParser._
  import ctx.{ definitions => defn }
  import ScalaModel.{
    DictionaryInvariant,
    TypeInvariant,
    TypeRef => ScalaTypeRef
  }

  private[scalats] def parseTypes(
      types: List[(Type, Tree)],
      symtab: Map[String, (Type, Tree)],
      examined: ListSet[TypeFullId],
      acceptsType: Symbol => Boolean
    ): Result[ListSet, TypeFullId] = parse(
    types,
    symtab,
    examined,
    acceptsType,
    ListSet.empty[ScalaModel.TypeDef]
  )

  // ---

  @annotation.tailrec
  private def parse(
      types: List[(Type, Tree)],
      symtab: Map[String, (Type, Tree)],
      examined: ListSet[TypeFullId],
      acceptsType: Symbol => Boolean,
      parsed: ListSet[ScalaModel.TypeDef]
    ): Result[ListSet, TypeFullId] = types match {
    case ((scalaType, _) :: tail) if !acceptsType(scalaType.typeSymbol) => {
      logger.debug(s"Type is excluded: ${scalaType.typeSymbol.fullName}")

      parse(tail, symtab, examined, acceptsType, parsed)
    }

    case (tpe @ (scalaType, tree)) :: tail => {
      val sym = tree.symbol // scalaType.typeSymbol
      val pos = sym.sourcePos

      val notDefined: Boolean = (pos != NoSourcePosition &&
        !compiled.contains(sym.source.file.canonicalPath))

      if (notDefined) {
        logger.debug(s"Postpone parsing of ${scalaType.typeSymbol.fullName} (${pos.source}:${pos.line}:${pos.column}) is not yet compiled")
      }

      inline def skip = parse(
        tail,
        symtab,
        /*res.*/ examined,
        acceptsType,
        parsed /* ++ res.parsed*/
      )

      if (
        scalaType.typeSymbol.isTypeParam ||
        notDefined
      ) {
        logger.debug(s"Skip not fully defined type: ${sym.fullName}")

        skip
      } else if (examined contains fullId(scalaType)) {
        // Skip already examined type (or a type parameter)
        // val res = parseType(scalaType, examined)
        logger.debug(s"Skip already examined type: ${sym.fullName}")

        skip
      } else {
        val typeArgs: List[Types.Type] = scalaType match {
          case Types.AppliedType(_, args) =>
            args

          case _ =>
            List.empty
        }

        val typeParams: Map[Symbol, Type] =
          typeArgs.map(t => t.typeSymbol -> t).toMap

        // is(Flags.Case)
        val memberTypes = scalaType.decls.iterator.flatMap { sym =>
          if (sym is Flags.Method) {
            if (sym.is(Flags.Case) && sym.info.isNullaryMethod) {
              val tpeSym = Denotations.staticRef(sym.signature.resSig).symbol
              val tpe = typeParams.getOrElse(tpeSym, tpeSym.info)

              List(sym.name.toString.trim -> tpe)
            } else {
              List.empty[(String, Type)]
            }
          } else if (sym.isTerm) {
            val tpe = sym.info
            val tpeSym = tpe.typeSymbol

            List(sym.name.toString.trim -> typeParams.getOrElse(tpeSym, tpe))
          } else {
            List.empty[(String, Type)]
          }
        }.toMap

        val members = collectValOrDefs(memberTypes, Seq(tree), Map.empty)
        val res = parseType(tpe, symtab, examined, acceptsType)

        val mappedTypeArgs = typeArgs.flatMap { st =>
          symtab.get(fullId(st).takeWhile(_ != '['))
        }

        parse(
          members ++: mappedTypeArgs ++: tail,
          symtab,
          res.examined,
          acceptsType,
          parsed ++ res.parsed
        )
      }
    }

    case _ =>
      Result(examined, parsed)
  }

  private lazy val enumerationTypeSym: Symbol = {

    val nme = Names.typeName("scala.Enumeration")
    val denot = Denotations.staticRef(nme)

    denot.symbol
  }

  private def parseType(
      tpe: (Type, Tree),
      symtab: Map[String, (Type, Tree)],
      examined: ListSet[TypeFullId],
      acceptsType: Symbol => Boolean
    ): Result[Option, TypeFullId] = {
    val tpeSym = tpe._1.typeSymbol

    import tpe.{ _1 => scalaType }

    if (
      tpeSym.fullName.startsWith("scala") &&
      !tpeSym.fullName.toString.startsWith("scala.Enumeration.")
    ) {
      logger.debug(s"Skip Scala type: ${tpeSym.fullName}")

      Result(examined + fullId(scalaType), Option.empty[ScalaModel.TypeDef])
    } else {
      val isModule = tpeSym.is(Flags.ModuleClass) // || tpeSym.is(Flags.Module)

      scalaType.dealias match {
        case _
            if (isModule &&
              scalaType.baseClasses.contains(enumerationTypeSym)) =>
          // TODO: Or Is provided Scala3 scala.deriving.Mirror.SumOf
          parseEnumeration(scalaType, examined)

        case _ if isModule =>
          parseObject(tpe, examined)

        case p: Types.TypeProxy if (tpeSym is Flags.Opaque) =>
          opaqueTypeAlias(p, tpeSym, examined)

        case Types.OrType(_, _) =>
          parseUnionType(scalaType, tpe._2, symtab, examined, acceptsType)

        case _ if (tpeSym.isClass) => {
          val classSym = tpeSym.asClass

          // TODO: Not sealed trait like CaseClass

          if (
            classSym.is(Flags.Trait) &&
            classSym.is(Flags.Sealed) &&
            scalaType.typeParams.isEmpty
          ) {
            parseSealedUnion(scalaType, tpe._2, symtab, examined, acceptsType)
          } else if (isAnyValChild(scalaType)) {
            parseValueClass(tpe, examined)
          } else if (isCaseClass(scalaType)) {
            parseCaseClass(tpe, examined)
          } else if (isEnumerationValue(scalaType)) {
            val e: Option[Types.Type] = scalaType match {
              case Types.TypeRef(e @ Types.TermRef(_, _), v)
                  if (v.toString == "class Value") =>
                Some(e)

              case _ =>
                None
            }

            e match {
              case Some(enumerationObject) =>
                parseEnumeration(enumerationObject, examined)

              case _ => {
                logger.debug(
                  s"Fails to resolve enumeration object: ${scalaType}"
                )

                Result(examined, Option.empty[ScalaModel.TypeDef])
              }
            }
          } else {
            Result(
              examined + fullId(scalaType),
              Option.empty[ScalaModel.TypeDef]
            )
          }
        }

        case _ => {
          logger.warning(s"Unsupported Scala type: ${tpeSym.fullName}")

          Result(examined + fullId(scalaType), Option.empty[ScalaModel.TypeDef])
        }
      }
    }
  }

  private object Field {

    def unapply(m: Symbol): Option[Symbol] =
      if (
        m.isTerm && m.isPublic &&
        !m.is(Flags.Abstract) && !m.is(Flags.Implicit) &&
        (!m.is(Flags.Method) || (m.info match {
          case mt: MethodType =>
            mt.paramNamess.forall(_.isEmpty)

          case _ =>
            false
        })) && {
          val n = m.name.toString
          !(n.contains("$") || n.startsWith("<"))
        } &&
        m.allOverriddenSymbols.forall { o =>
          !o.is(Flags.JavaDefined) && !o.owner.fullName.toString.startsWith(
            "scala."
          )
        }
      ) {
        Some(m)
      } else {
        None
      }
  }

  private val skipCompanion = true // TODO: (low priority) Configurable

  @inline private def isLiteralType(tpe: Type): Boolean =
    isAnyValChild(tpe) || tpe <:< defn.StringClass.info

  @annotation.tailrec
  private def appliedOp(
      op: Names.SimpleName,
      excludedSymbols: Seq[String],
      application: List[Tree],
      out: List[Tree]
    ): List[Tree] = application match {
    case tree :: tail =>
      tree match {
        case Apply(o, args) if (o.symbol.name.toString == op.toString) =>
          appliedOp(op, excludedSymbols, o :: args ::: tail, out)

        case TypeApply(o, _) =>
          appliedOp(op, excludedSymbols, o :: tail, out)

        case s @ Select(o, `op`) =>
          appliedOp(op, excludedSymbols, o :: tail, out)

        case This(Ident(_)) | TypeTree() =>
          appliedOp(op, excludedSymbols, tail, out)

        case s @ Select(_, _)
            if (
              s.symbol.is(Flags.Method) && !s.tpe.isNullaryMethod
            ) =>
          appliedOp(op, excludedSymbols, tail, s :: out)

        case _ if excludedSymbols.contains(tree.symbol.fullName) =>
          appliedOp(op, excludedSymbols, tail, out)

        case _ =>
          appliedOp(op, excludedSymbols, tail, tree :: out)
      }

    case _ =>
      out.reverse

  }

  private lazy val plainPrinter =
    new dotty.tools.dotc.printing.PlainPrinter(ctx)

  private lazy val printerCtx: Context = {
    import dotty.tools.dotc.config._

    val freshCtx = ctx.fresh

    freshCtx.setSettings(
      freshCtx.settings.color.update("never")(using freshCtx)
    )

    freshCtx
  }

  inline private def constantValue(v: Constants.Constant): String =
    v.tag match {
      case Constants.StringTag =>
        v.show(using printerCtx)

      case _ =>
        v.value.toString
    }

  private def simpleTypeInvariant(
      k: String,
      owner: Tree,
      rhs: Tree,
      hint: Option[ScalaTypeRef]
    ): Option[TypeInvariant.Simple] = rhs match {
    case Literal(v) => {
      // Literal elements in list are not defined with own symbol
      val symbol = Option(owner.symbol)

      def mt: Type = symbol.collect {
        case sym if sym.info.isNullaryMethod =>
          Denotations.staticRef(sym.signature.resSig).info

      }.getOrElse(owner.tpe)

      def productMember = owner.symbol.allOverriddenSymbols
        .exists(_.owner.fullName.toString startsWith "scala.Product")

      if (k.startsWith("product") && productMember) {
        None
      } else {
        Some(
          ScalaModel.LiteralInvariant(
            name = k,
            typeRef = hint getOrElse scalaTypeRef(mt.dealias, Set.empty),
            value = constantValue(v)
          )
        )
      }
    }

    case Apply(_, (l @ Literal(_)) :: Nil) =>
      simpleTypeInvariant(k, owner, l, hint)

    case i @ Ident(nme) => {
      val term = i.symbol.owner.requiredMethod(nme)

      val tpeRef = scalaTypeRef(term.info.dealias, Set.empty) match {
        case unknown @ ScalaModel.UnknownTypeRef(_) =>
          hint.getOrElse(unknown)

        case tr =>
          tr
      }

      Some(
        ScalaModel.SelectInvariant(
          name = k,
          typeRef = tpeRef,
          qualifier = ScalaModel.ThisTypeRef,
          term = nme.toString
        )
      )
    }

    case s @ Select(q, nme) if (q.isTerm && ({
          val sym = s.symbol
          val qual = q.symbol

          sym.isPublic &&
          (sym.isTerm ||
            (sym.is(Flags.Method) && sym.signature.paramsSig.isEmpty)) &&
          qual.isPublic &&
          (qual.is(Flags.Module) || qual.is(Flags.ModuleClass))
        })) => {
      // Stable reference; e;g. x = qual.y

      val qualTpe = q match {
        case This(_) =>
          ScalaModel.ThisTypeRef

        case _ =>
          scalaTypeRef(q.tpe.dealias, Set.empty)
      }

      val tpeRef = scalaTypeRef(s.tpe.dealias, Set.empty) match {
        case unknown @ ScalaModel.UnknownTypeRef(_) =>
          hint.getOrElse(unknown)

        case tr =>
          tr
      }

      Some(
        ScalaModel.SelectInvariant(
          name = k,
          typeRef = tpeRef,
          qualifier = qualTpe,
          term = nme.toString // TODO: Review q.name?
        )
      )
    }

    case _ =>
      None
  }

  private lazy val SetType: Type =
    Symbols.requiredClass("scala.collection.immutable.Set").typeRef

  private lazy val SeqType: Type =
    Symbols.requiredClass("scala.collection.immutable.Seq").typeRef

  private lazy val MapType: Type =
    Symbols.requiredClass("scala.collection.immutable.Map").typeRef

  private lazy val ProductType: Type =
    Symbols.requiredClass("scala.Product").typeRef

  private object WithTypeArgs {

    def unapply(tree: Tree): Option[(Tree, List[Type])] = tree.tpe match {
      case Types.AppliedType(_, args) =>
        Some(tree -> args)

      case _ =>
        None
    }
  }

  private lazy val plusplus = Names.termName("++")

  private def typeInvariant(
      k: String,
      owner: Tree,
      rhs: Tree,
      hint: Option[ScalaTypeRef]
    ): Option[TypeInvariant] = rhs match {

    case WithTypeArgs(
          app @ Apply(_, Typed(SeqLiteral(args, _), _) :: Nil),
          tpeArgs @ (ktpe :: vtpe :: Nil)
        ) if (app.tpe <:< MapType.appliedTo(tpeArgs)) => {
      // Dictionary
      val entries: Map[TypeInvariant.Simple, TypeInvariant] =
        args.zipWithIndex
          .collect(
            Function.unlift[
              (Tree, Int),
              (TypeInvariant.Simple, TypeInvariant)
            ] {
              case (ArrowedTuple((ky, v)), idx) => {
                for {
                  key <- simpleTypeInvariant(
                    s"${k}.${idx}",
                    ky,
                    ky,
                    None
                  )

                  vlu <- typeInvariant(s"${k}[${idx}]", v, v, None)
                } yield key -> vlu
              }

              case entry =>
                logger.warning(s"Unsupported dictionary entry: ${entry}")
                None
            }
          )
          .toMap

      if (entries.nonEmpty) {
        Some(
          DictionaryInvariant(
            name = k,
            keyTypeRef = scalaTypeRef(ktpe, Set.empty),
            valueTypeRef = scalaTypeRef(vtpe, Set.empty),
            entries = entries
          )
        )
      } else {
        logger.warning(s"Skip empty dictionary: $k")

        None
      }
    }

    case WithTypeArgs(
          app @ Apply(a, Typed(SeqLiteral(args, _), _) :: Nil),
          vtpe :: Nil
        ) if (app.tpe <:< SeqType.appliedTo(vtpe)) => {
      // Seq/List factory

      val elements = args.zipWithIndex.collect(
        Function.unlift[(Tree, Int), TypeInvariant] {
          case (e, i) => typeInvariant(s"${k}[${i}]", e, e, None)
        }
      )

      if (elements.nonEmpty && elements.size == args.size) {
        val elmTpe = scalaTypeRef(vtpe, Set.empty)

        Some(
          ScalaModel.ListInvariant(
            name = k,
            typeRef = ScalaModel.CollectionRef(elmTpe),
            valueTypeRef = elmTpe,
            values = elements
          )
        )
      } else {
        logger.warning(s"Skip list with non-stable value: $k")

        None
      }
    }

    case WithTypeArgs(app @ Apply(a, args), vtpe :: Nil)
        if (app.tpe <:< SeqType.appliedTo(vtpe) &&
          a.symbol.name.toString == "++") =>
      scalaTypeRef(app.tpe, Set.empty) match {
        case colTpe @ ScalaModel.CollectionRef(valueTpe) => {
          val terms = appliedOp(plusplus, List.empty, List(app), List.empty)

          val elements = terms.zipWithIndex
            .collect(
              Function.unlift[(Tree, Int), TypeInvariant] {
                case (e, i) =>
                  typeInvariant(s"${k}[${i}]", e, e, hint = Some(colTpe))
              }
            )
            .toSet

          if (elements.size != terms.size || elements.size < 2) {
            logger.warning(s"Skip merged list with non-stable value: $k")

            None
          } else {
            Some(
              ScalaModel.MergedListsInvariant(
                name = k,
                valueTypeRef = valueTpe,
                children = elements.toList
              )
            )
          }
        }

        case _ =>
          None
      }

    case WithTypeArgs(
          app @ Apply(a, Typed(SeqLiteral(args, _), _) :: Nil),
          vtpe :: Nil
        ) if ({
          app.tpe <:< SetType.appliedTo(vtpe) &&
          a.symbol.name.toString == "apply" && args.nonEmpty
        }) => {
      // Set factory

      val elements = args.zipWithIndex
        .collect(
          Function.unlift[(Tree, Int), TypeInvariant] {
            case (e, i) =>
              typeInvariant(s"${k}[${i}]", e, e, None)
          }
        )
        .toSet

      if (elements.nonEmpty && elements.size == args.size) {
        val elmTpe = scalaTypeRef(vtpe, Set.empty)

        Some(
          ScalaModel.SetInvariant(
            name = k,
            typeRef = ScalaModel.CollectionRef(elmTpe),
            valueTypeRef = elmTpe,
            values = elements
          )
        )
      } else {
        logger.warning(s"Skip list with non-stable value: $k")

        None
      }
    }

    case WithTypeArgs(app @ Apply(a, _), vtpe :: Nil)
        if (app.tpe <:< SetType.appliedTo(vtpe) &&
          a.symbol.name.toString == "++") =>
      scalaTypeRef(app.tpe, Set.empty) match {
        case colTpe @ ScalaModel.CollectionRef(valueTpe) => {
          val terms = appliedOp(plusplus, List.empty, List(app), List.empty)

          val elements = terms.zipWithIndex
            .collect(
              Function.unlift[(Tree, Int), TypeInvariant] {
                case (e, i) =>
                  typeInvariant(s"${k}[${i}]", e, e, hint = Some(colTpe))
              }
            )
            .toSet

          if (elements.size != terms.size || elements.size < 2) {
            logger.warning(s"Skip merged set with non-stable value: $k")

            None
          } else {
            Some(
              ScalaModel.MergedSetsInvariant(
                name = k,
                valueTypeRef = valueTpe,
                children = elements.toList
              )
            )
          }
        }

        case _ =>
          None
      }

    case _ => {
      val unionType: Option[ScalaTypeRef] = owner match {
        case tr: ValOrDefDef =>
          tr.tpt.tpe.dealias match {
            case or @ Types.OrType(_, _) =>
              Some(scalaTypeRef(or, Set.empty))

            case _ =>
              Option.empty[ScalaTypeRef]
          }

        case _ =>
          Option.empty[ScalaTypeRef]
      }

      val typeHint = hint orElse unionType

      simpleTypeInvariant(k, owner, rhs, typeHint)
    }
  }

  @SuppressWarnings(Array("UnsafeTraversableMethods" /*tail*/ ))
  @annotation.tailrec
  private def typeInvariants(
      owner: Type,
      declNames: ListSet[String],
      forest: Seq[Tree],
      vs: List[TypeInvariant]
    ): ListSet[TypeInvariant] = {
    forest.headOption match {
      case Some(TypeDef(_, rhs)) =>
        typeInvariants(
          owner,
          declNames,
          rhs +: forest.tail,
          vs
        )

      case Some(t @ Template(c, _, _, _)) =>
        typeInvariants(
          owner,
          declNames,
          c +: t.body ++: forest.tail,
          vs
        )

      case Some(tr: ValOrDefDef) if (!tr.name.startsWith("<")) => {
        val k = tr.name.toString.trim

        if (declNames contains k) {
          typeInvariant(k, tr, tr.rhs, None) match {
            case Some(single) =>
              typeInvariants(
                owner,
                declNames,
                /*collectSubTrees[Seq](tr) ++: */ forest.tail,
                single :: vs
              )

            case _ =>
              typeInvariants(
                owner,
                declNames,
                /*collectSubTrees[Seq](tr) ++: */ forest.tail,
                vs
              )
          }
        } else {
          typeInvariants(
            owner,
            declNames,
            /*collectSubTrees[Seq](tr) ++: */ forest.tail,
            vs
          )
        }
      }

      case Some(Apply(o, args)) =>
        typeInvariants(
          owner,
          declNames,
          o +: args ++: forest.tail,
          vs
        )

      case Some(TypeApply(o, _)) =>
        typeInvariants(
          owner,
          declNames,
          o +: forest.tail,
          vs
        )

      case Some(Select(o, _)) =>
        typeInvariants(
          owner,
          declNames,
          o +: forest.tail,
          vs
        )

      case Some(This(Ident(_)) | TypeTree()) =>
        typeInvariants(
          owner,
          declNames,
          forest.tail,
          vs
        )

      case Some(tr) =>
        typeInvariants(
          owner,
          declNames,
          /*collectSubTrees[Seq](tr) ++: */ forest.tail,
          vs
        )

      case _ =>
        ListSet.empty ++ vs.reverse
    }
  }

  private def parseObject(
      tpe: (Type, Tree),
      examined: ListSet[TypeFullId]
    ): Result[Option, TypeFullId] = {
    import tpe.{ _1 => scalaType }

    def classExists: Boolean = scalaType.typeSymbol.linkedClass.exists

    if (skipCompanion && classExists) {
      logger.debug(s"Skip companion object: ${scalaType.typeSymbol.fullName}")

      Result(examined + fullId(scalaType), Option.empty[ScalaModel.TypeDef])
    } else {
      lazy val memberNames: Set[Names.Name] =
        scalaType.memberNames(Types.fieldFilter)

      def ctorInvariants(
          mt: Types.MethodType,
          args: List[Tree]
        ): List[ScalaModel.TypeInvariant] = {
        if (args.size != mt.paramNames.size) {
          List.empty
        } else {
          mt.paramNames.zip(mt.paramInfos).zip(args).collect {
            case (
                  (nme, pt),
                  Literal(v @ Constants.Constant(_))
                ) if (memberNames contains nme) =>
              ScalaModel.LiteralInvariant(
                name = nme.toString,
                typeRef = scalaTypeRef(pt, Set.empty),
                value = constantValue(v)
              )
          }
        }
      }

      @annotation.tailrec
      @SuppressWarnings(Array("UnsafeTraversableMethods" /*tail*/ ))
      def fromCtor(trees: Seq[Tree]): List[ScalaModel.TypeInvariant] =
        trees.headOption match {
          case Some(
                Template(
                  DefDef(_, Nil :: Nil, _, _),
                  (a @ Apply(Select(New(_), _), args)) :: Nil,
                  _,
                  _
                )
              ) => {
            a.symbol.owner.info.decls.filter(_.isConstructor).toList match {
              case ctor :: Nil =>
                ctor.info match {
                  case mt: Types.MethodType =>
                    ctorInvariants(mt, args)

                  case _ =>
                    logger.debug(s"Fails to find valid constructor: ${scalaType.typeSymbol.fullName}")

                    List.empty
                }

              case alts => {
                val argTypes = args.map(_.tpe)
                val ctor = alts.collectFirst(
                  Function.unlift[Symbols.Symbol, Types.MethodType] { ctor =>
                    ctor.info match {
                      case mt: Types.MethodType => {
                        val mps = mt.paramInfos

                        if (mps.size != argTypes.size) {
                          None
                        } else {
                          val matches = mps.zip(argTypes).forall {
                            case (ma, arg) => arg <:< ma
                          }

                          if (matches) Some(mt)
                          else None
                        }
                      }

                      case _ =>
                        None
                    }
                  }
                )

                ctor match {
                  case Some(mt) =>
                    ctorInvariants(mt, args)

                  case _ =>
                    logger.debug(s"Fails to find ${scalaType.typeSymbol.fullName} constructor matching ${argTypes.map(_.typeSymbol.fullName)}")

                    List.empty
                }
              }
            }
          }

          case Some(t) =>
            fromCtor(collectSubTrees[Seq](t) ++: trees.tail)

          case _ =>
            List.empty
        }

      val values = fromCtor(Seq(tpe._2))

      val exprNames = scalaType.decls.toList.collect(
        Function.unlift[Symbols.Symbol, String] { d =>
          d.info match {
            case Types.ExprType(e) =>
              Some(d.name.toString)

            case _ =>
              Option.empty[String]
          }
        }
      )

      val decls: ListSet[String] =
        ListSet.empty ++ (memberNames.map(_.toString) -- values.map(
          _.name
        )) ++ exprNames

      val invariants = typeInvariants(scalaType, decls, Seq(tpe._2), values)
      val identifier = buildQualifiedIdentifier(scalaType.typeSymbol)

      Result(
        examined = (examined + fullId(scalaType)),
        parsed = Some[ScalaModel.TypeDef](
          ScalaModel.CaseObject(
            identifier.copy(name = identifier.name stripSuffix f"$$"),
            invariants
          )
        )
      )
    }
  }

  @annotation.tailrec
  private def parseOr(in: List[Type], out: List[Type]): List[Type] =
    in.headOption match {
      case Some(Types.OrType(a, b)) =>
        parseOr(a :: b :: in.tail, out)

      case Some(o) =>
        parseOr(in.tail, o :: out)

      case _ =>
        out.reverse
    }

  private def parseUnionType(
      tpe: Type,
      tree: Tree,
      symtab: Map[String, (Type, Tree)],
      examined: ListSet[TypeFullId],
      acceptsType: Symbol => Boolean
    ): Result[Option, TypeFullId] = {

    val ps = parseOr(List(tpe.dealias), List.empty)

    val possibilities = ps.flatMap { pt => symtab.get(fullId(pt)) }

    if (possibilities.size != ps.size) {
      val pos = tree.symbol.sourcePos

      logger.debug(s"Postpone parsing of union type ${tpe.typeSymbol.fullName} (${pos.source}:${pos.line}:${pos.column}) has subclasses are not yet fully defined")

      Result(
        examined = examined,
        parsed = Option.empty[ScalaModel.TypeDef]
      )
    } else {
      val res = parse(
        possibilities,
        symtab,
        examined + fullId(tpe), {
          val psSyms = possibilities.map(_._1.typeSymbol)

          { (tpeSym: Symbol) => psSyms.contains(tpeSym) && acceptsType(tpeSym) }
        },
        ListSet.empty[ScalaModel.TypeDef]
      )

      Result(
        examined = res.examined,
        parsed = Some[ScalaModel.TypeDef](
          ScalaModel.SealedUnion(
            identifier = buildQualifiedIdentifier(tpe.typeSymbol),
            fields = ListSet.empty,
            possibilities = res.parsed
          )
        )
      )
    }
  }

  private def parseSealedUnion(
      tpe: Type,
      tree: Tree,
      symtab: Map[String, (Type, Tree)],
      examined: ListSet[TypeFullId],
      acceptsType: Symbol => Boolean
    ): Result[Option, TypeFullId] = {
    // TODO: (low priority) Check & warn there is no type parameters for a union type

    // Members
    lazy val memberTypes = tpe.decls.iterator.collect {
      case m
          if (m.isPublic && !m.name.toString.startsWith("<") && m.isTerm && (!m
            .is(
              Flags.Method
            ) || m.signature.paramsSig.isEmpty)) =>
        m.name.toString.trim -> m.info
    }.toMap

    lazy val members =
      collectValOrDefs(memberTypes, Seq(tree), Map.empty).collect {
        // TODO: Remove abstract to resolve typeinvariant?
        case (vtpe, vdef) if (vdef.rhs.isEmpty /* is abstract */ ) =>
          member(vdef.symbol, List.empty)
      }

    directKnownSubclasses(tpe) match {
      case ps @ (_ :: _) => {
        val possibilities = ps.flatMap { pt => symtab.get(fullId(pt)) }

        if (possibilities.size != ps.size) {
          val pos = tree.symbol.sourcePos

          logger.debug(s"Postpone parsing of sealed union ${tpe.typeSymbol.fullName} (${pos.source}:${pos.line}:${pos.column}) has subclasses are not yet fully defined")

          Result(
            examined = examined,
            parsed = Option.empty[ScalaModel.TypeDef]
          )
        } else {
          val res = parse(
            possibilities,
            symtab,
            examined + fullId(tpe), {
              val psSyms = possibilities.map(_._1.typeSymbol)

              { (tpeSym: Symbol) =>
                psSyms.contains(tpeSym) && acceptsType(tpeSym)
              }
            },
            ListSet.empty[ScalaModel.TypeDef]
          )

          Result(
            examined = res.examined,
            parsed = Some[ScalaModel.TypeDef](
              ScalaModel.SealedUnion(
                identifier = buildQualifiedIdentifier(tpe.typeSymbol),
                fields = ListSet.empty ++ members,
                possibilities = res.parsed
              )
            )
          )
        }
      }

      case _ =>
        Result(
          examined = examined + fullId(tpe),
          parsed = Option.empty[ScalaModel.TypeDef]
        )
    }
  }

  // TODO: Support new Scala3 enum
  private def parseEnumeration(
      enumerationType: Type,
      examined: ListSet[TypeFullId]
    ): Result[Option, TypeFullId] = {
    val enumerationObject = enumerationType.typeSymbol

    val identifier = {
      val id = buildQualifiedIdentifier(enumerationObject)
      id.copy(name = id.name.stripSuffix(f"$$"))
    }

    lazy val enumerationValueSym =
      enumerationType.member(Names typeName "Value")

    val values = enumerationType.decls.filter {
      case decl => decl.isPublic && decl.info.isValueType
    }.map(_.asTerm.name.toString.trim)

    Result(
      examined = (examined +
        fullId(enumerationValueSym.info) +
        fullId(enumerationType)),
      parsed = Some[ScalaModel.TypeDef](
        ScalaModel.EnumerationDef(identifier, ListSet(values.toSeq: _*))
      )
    )
  }

  private def opaqueTypeAlias(
      tpe: Types.TypeProxy,
      tpeSym: Symbol,
      examined: ListSet[TypeFullId]
    ): Result[Option, TypeFullId] = Result(
    examined = examined + fullId(tpe),
    parsed = Some[ScalaModel.TypeDef](
      ScalaModel.ValueClass(
        buildQualifiedIdentifier(tpeSym),
        new ScalaModel.TypeMember(
          "",
          scalaTypeRef(tpe.translucentSuperType, Set.empty)
        )
      )
    )
  )

  private def parseValueClass(
      tpe: (Type, Tree),
      examined: ListSet[TypeFullId]
    ): Result[Option, TypeFullId] = {
    import tpe.{ _1 => valueClassType }

    val m = valueClassType.decls.filter(!_.is(Flags.Method)).collectFirst {
      case sym if sym.isTerm =>
        new ScalaModel.TypeMember(
          sym.name.toString.trim,
          scalaTypeRef(sym.info.dealias, Set.empty)
        )

    }

    m match {
      case Some(vm) =>
        Result(
          examined = examined + fullId(valueClassType),
          parsed = Some[ScalaModel.TypeDef](
            ScalaModel.ValueClass(
              buildQualifiedIdentifier(valueClassType.typeSymbol),
              vm
            )
          )
        )

      case _ => {
        logger.warning(s"Unsupported Value class: ${valueClassType}")

        Result(
          examined = examined + fullId(valueClassType),
          parsed = Option.empty[ScalaModel.TypeDef]
        )
      }
    }
  }

  // TODO: Parse default field values
  private def parseCaseClass(
      tpe: (Type, Tree),
      examined: ListSet[TypeFullId]
    ): Result[Option, TypeFullId] = {

    import tpe.{ _1 => caseClassType }

    val typeParams = caseClassType.typeConstructor.dealias.typeParams
      .map(_.paramName.mangledString)

    val declNames: ListSet[String] =
      ListSet.empty ++ caseClassType.decls.toList.collect {
        case Field(m) => // ?is Flags.Method
          m.name.toString
      }

    val values = typeInvariants(tpe._1, declNames, Seq(tpe._2), List.empty)

    // Members
    val members = caseClassType.decls.toList.collect {
      case Field(m)
          if (!values.exists(
            _.name == m.name.toString.trim
          )) => // m.is(Flags.Method) && m.is(Flags.Case) &&
        m.name.toString.trim -> member(m, typeParams)
    }.toMap

    // Make sure the declaration order is respected
    val orderedMembers: ListSet[ScalaModel.TypeMember] =
      ListSet.empty ++ declNames.collect(
        Function.unlift(members.lift)
      ) ++ (members -- declNames).values

    Result(
      examined = examined + fullId(caseClassType),
      parsed = Some[ScalaModel.TypeDef](
        ScalaModel.CaseClass(
          buildQualifiedIdentifier(caseClassType.typeSymbol),
          orderedMembers,
          ListSet.empty ++ values,
          typeParams
        )
      )
    )
  }

  @inline private def member(
      sym: Symbol,
      typeParams: List[String]
    ) = new ScalaModel.TypeMember(
    sym.name.toString,
    scalaTypeRef(
      sym.denot.info.dealias,
      typeParams.toSet
    )
  )

  // ---

  private object ArrowedTuple {
    private val arrow = Names.termName("->")

    def unapply(tree: Tree): Option[(Tree, Tree)] =
      tree match {
        case app @ Apply(
              TypeApply(
                Select(Apply(TypeApply(ident, _), k :: Nil), `arrow`),
                _
              ),
              v :: Nil
            ) if (ident.show == "ArrowAssoc") =>
          Some(k -> v)

        case _ =>
          None
      }
  }

  private lazy val IterableType: Type =
    Symbols.requiredClass("scala.collection.Iterable").typeRef

  private lazy val OptionType: Type =
    Symbols.requiredClass("scala.Option").typeRef

  private lazy val Tuple1Type: Type =
    Symbols.requiredClass("scala.Tuple1").typeRef

  private def scalaTypeRef(
      scalaType: Type,
      typeParams: Set[String]
    ): ScalaTypeRef = {
    import scalaType.typeSymbol
    val tpeName: String = typeSymbol.name.toString

    def unknown =
      ScalaModel.UnknownTypeRef(buildQualifiedIdentifier(typeSymbol))

    lazy val isAnyVal = isAnyValChild(scalaType)

    def nonGenericType = scalaType match {
      case Scalar(ref) =>
        ref

      case or @ Types.OrType(_, _) =>
        ScalaModel.UnionRef(
          ListSet.empty ++ parseOr(List(or), List.empty)
            .map(scalaTypeRef(_, Set.empty))
        )

      case _ =>
        tpeName match {
          case typeParam if (typeParams contains typeParam) =>
            ScalaModel.TypeParamRef(typeParam)

          case _ if isAnyVal =>
            // #ValueClass_1
            scalaType.decls
              .filter(!_.is(Flags.Method))
              .map(_.info)
              .headOption match {
              case Some(valueTpe) =>
                ScalaModel.TaggedRef(
                  identifier = buildQualifiedIdentifier(typeSymbol),
                  tagged = scalaTypeRef(valueTpe, Set.empty)
                )

              case _ =>
                unknown
            }

          case _ if isEnumerationValue(scalaType) => {
            val id = scalaType match {
              case Types.TypeRef(t @ Types.TermRef(_, _), _) => {
                val n = t.info.typeSymbol.fullName.toString.stripSuffix(f"$$")

                if (n startsWith f"$$wrapper._$$") {
                  f"$$wrapper.expr.${n drop 11}"
                } else {
                  n
                }
              }

              case Types.TypeRef(m, v) if (v.toString == "class Value") =>
                m.typeSymbol.fullName.toString.stripSuffix(f"$$")

              case _ =>
                scalaType.typeSymbol.fullName.toString
            }

            val qualId = id.split("\\.").toList.reverse match {
              case nme :: rev =>
                ScalaModel.QualifiedIdentifier(
                  name = nme,
                  enclosingClassNames = rev.reverse
                )

              case _ =>
                ScalaModel.QualifiedIdentifier(
                  name = scalaType.typeSymbol.fullName.toString,
                  enclosingClassNames = List.empty
                )
            }

            ScalaModel.EnumerationRef(qualId)
          }

          case _ =>
            unknown
        }
    }

    // val typeArgs = scalaType.typeSymbol.typeParams.map(_.info)

    val isCaseCls = isCaseClass(scalaType)

    scalaType match {
      case Types.AppliedType(_, typeArgs) if typeArgs.nonEmpty =>
        if (isCaseCls) {
          val caseClassName = buildQualifiedIdentifier(typeSymbol)
          val typeArgRefs = typeArgs.map(scalaTypeRef(_, typeParams))

          ScalaModel.CaseClassRef(caseClassName, typeArgRefs)
        } else {
          typeArgs match {
            case args @ (a :: b :: _) =>
              tpeName match {
                case "Either" => // TODO: (medium priority) Check type
                  ScalaModel.UnionRef(
                    ListSet(
                      scalaTypeRef(a, typeParams),
                      scalaTypeRef(b, typeParams)
                    )
                  )

                case "Map" =>
                  ScalaModel.MapRef(
                    scalaTypeRef(a, typeParams),
                    scalaTypeRef(b, typeParams)
                  )

                case _
                    if (typeSymbol.fullName.toString startsWith "scala.Tuple") =>
                  ScalaModel.TupleRef(
                    args.map(a => scalaTypeRef(a, typeParams))
                  )

                case _ =>
                  unknown
              }

            case innerType :: _
                if (scalaType <:< OptionType.appliedTo(innerType)) =>
              ScalaModel.OptionRef(scalaTypeRef(innerType, typeParams))

            case innerType :: _
                if (
                  scalaType <:< IterableType.appliedTo(innerType)
                ) =>
              ScalaModel.CollectionRef(scalaTypeRef(innerType, typeParams))

            case innerType :: _
                if (scalaType <:< Tuple1Type.appliedTo(innerType)) =>
              ScalaModel.TupleRef(List(scalaTypeRef(innerType, typeParams)))

            case args if (typeSymbol.fullName startsWith "scala.Tuple") =>
              ScalaModel.TupleRef(args.map(a => scalaTypeRef(a, typeParams)))

            case _ =>
              nonGenericType

          }
        }

      case _ =>
        if (isCaseCls && !isAnyVal) {
          val caseClassName = buildQualifiedIdentifier(typeSymbol)

          ScalaModel.CaseClassRef(caseClassName, List.empty)
        } else {
          nonGenericType
        }
    }
  }

  private object Scalar {

    // TODO: (medium priority) Check type symbol
    def unapply(scalaType: Type): Option[ScalaTypeRef] = {
      val tpeName: String = scalaType.typeSymbol.name.toString

      (scalaType.typeSymbol.fullName.toString -> tpeName) match {
        case (_, "Int" | "Byte" | "Short") =>
          Some(ScalaModel.IntRef)

        case (_, "Long") =>
          Some(ScalaModel.LongRef)

        case (_, "Float" | "Double") =>
          Some(ScalaModel.DoubleRef)

        case (_, "BigDecimal") =>
          Some(ScalaModel.BigDecimalRef)

        case (_, "BigInt" | "BigInteger") =>
          Some(ScalaModel.BigIntegerRef)

        case (_, "Boolean") =>
          Some(ScalaModel.BooleanRef)

        case (_, "String") =>
          Some(ScalaModel.StringRef)

        case ("java.util.UUID", _) =>
          Some(ScalaModel.UuidRef)

        case ("java.time.LocalDate", _) =>
          Some(ScalaModel.DateRef)

        case (
              full,
              "Instant" | "LocalDateTime" | "ZonedDateTime" | "OffsetDateTime"
            ) if (full startsWith "java.time.") =>
          Some(ScalaModel.DateTimeRef)

        case (full, "Date" | "Timestamp") if (full startsWith "java.sql") =>
          Some(ScalaModel.DateTimeRef)

        case ("java.util.Date", _) =>
          Some(ScalaModel.DateTimeRef)

        case _ =>
          None
      }
    }
  }

  @inline private def isCaseClass(scalaType: Type): Boolean = {
    val sym = scalaType.typeSymbol

    !isAnyValChild(scalaType) &&
    sym.isClass && sym.is(Flags.Case) &&
    !sym.fullName.startsWith("scala") /* e.g. Skip Tuple */
  }

  @inline private def isAnyValChild(scalaType: Type): Boolean =
    scalaType <:< defn.AnyValClass.info || scalaType.baseClasses.exists(
      _.fullName.toString == "scala.AnyVal"
    )

  @inline private def isEnumerationValue(scalaType: Type): Boolean = {
    // TODO: (low priority) rather compare Type (than string)
    val sym = scalaType.typeSymbol
    sym.isClass && sym.asClass.fullName.toString == "scala.Enumeration.Value"
  }

  @annotation.tailrec
  private def ownerChain(
      symbol: Symbol,
      acc: List[Symbol] = List.empty
    ): List[Symbol] = {
    if (!symbol.exists || symbol.owner.is(Flags.Package)) acc
    else ownerChain(symbol.owner, symbol.owner +: acc)
  }

  @inline private def buildQualifiedIdentifier(
      symbol: Symbol
    ): ScalaModel.QualifiedIdentifier =
    ScalaModel.QualifiedIdentifier(
      name = symbol.name.toString.stripSuffix(f"$$"),
      enclosingClassNames =
        ownerChain(symbol).map(_.name.toString stripSuffix f"$$")
    )

  private def directKnownSubclasses(tpe: Type): List[Type] = {
    val tpeSym = tpe.typeSymbol.asClass

    if (tpeSym.is(Flags.Sealed) && tpeSym.is(Flags.Trait)) {
      tpeSym.sealedStrictDescendants.map(_.info)
    } else List.empty
  }

  @inline private def fullId(scalaType: Type): String = {
    val typeArgs = scalaType.paramInfoss.flatten.map(fullId)
    val n = scalaType.typeSymbol.fullName.toString

    if (typeArgs.isEmpty) n
    else n + typeArgs.mkString("[", ", ", "]")
  }

  private def collectSubTrees[M[_]](
      tree: Tree
    )(using
      cbf: Factory[Tree, M[Tree]]
    ): M[Tree] = {
    val subTrees = cbf.newBuilder

    tree.foreachSubTree { sub =>
      if (sub != tree) {
        subTrees += sub
      }
    }

    subTrees.result()
  }

  @annotation.tailrec
  private def collectValOrDefs(
      memberTypes: Map[String, Type],
      forest: Seq[Tree],
      syms: Map[String, (Type, ValOrDefDef)]
    ): List[(Type, ValOrDefDef)] = forest.headOption match {
    case Some(tr: ValOrDefDef) => {
      val k = tr.name.toString.trim

      if (!syms.contains(k)) {
        memberTypes.get(k) match {
          case Some(tp) =>
            collectValOrDefs(memberTypes, forest.tail, syms + (k -> (tp -> tr)))

          case _ =>
            collectValOrDefs(memberTypes, forest.tail, syms)
        }
      } else {
        collectValOrDefs(memberTypes, forest.tail, syms)
      }
    }

    case Some(TypeDef(_, rhs)) =>
      collectValOrDefs(memberTypes, rhs +: forest.tail, syms)

    case Some(t: Template) =>
      collectValOrDefs(memberTypes, t.body ++: forest.tail, syms)

    case Some(tr) =>
      collectValOrDefs(
        memberTypes,
        /*collectSubTrees[Seq](tr) ++: */ forest.tail,
        syms
      )

    case _ =>
      syms.values.toList
  }
}

private[scalats] object ScalaParser {
  import scala.language.higherKinds

  case class Result[M[_], Tpe](
      examined: ListSet[Tpe],
      parsed: M[ScalaModel.TypeDef])

  type TypeFullId = String
}
