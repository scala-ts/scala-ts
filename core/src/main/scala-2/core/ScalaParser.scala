package io.github.scalats.core

import scala.util.control.NonFatal

import scala.reflect.api.Universe

import io.github.scalats.scala.{ TypeRef => ScalaTypeRef, _ }

import Internals.ListSet

final class ScalaParser[Uni <: Universe](
    val universe: Uni,
    compiled: Set[String],
    logger: Logger
  )(implicit
    cu: CompileUniverse[Uni]) {

  import universe.{
    appliedType,
    ClassSymbolTag,
    ApplyTag,
    LiteralTag,
    MethodSymbol,
    MethodSymbolTag,
    ModuleSymbolTag,
    NullaryMethodTypeTag,
    Symbol,
    symbolOf,
    RefinedTypeTag,
    TermSymbolTag,
    Tree,
    Type,
    TypeName,
    TypeRefTag,
    typeOf,
    ValOrDefDefTag
  }

  private lazy val mirror = cu.defaultMirror(universe)

  import ScalaParser.{ Result, TypeFullId }

  private[scalats] def parseTypes(
      types: List[(Type, Tree)],
      symtab: Map[String, (Type, Tree)],
      examined: ListSet[TypeFullId],
      acceptsType: Symbol => Boolean
    ): Result[ListSet, TypeFullId] =
    parse(types, symtab, examined, acceptsType, ListSet.empty[TypeDef])

  // ---

  @annotation.tailrec
  private def parse(
      types: List[(Type, Tree)],
      symtab: Map[String, (Type, Tree)],
      examined: ListSet[TypeFullId],
      acceptsType: Symbol => Boolean,
      parsed: ListSet[TypeDef]
    ): Result[ListSet, TypeFullId] = types match {
    case ((scalaType, _) :: tail) if (!acceptsType(scalaType.typeSymbol)) => {
      logger.debug(s"Type is excluded: ${scalaType}")

      parse(tail, symtab, examined, acceptsType, parsed)
    }

    case (tpe @ (scalaType, tree)) :: tail => {
      val pos = scalaType.typeSymbol.pos
      val notDefined: Boolean = (pos != universe.NoPosition &&
        !compiled.contains(pos.source.file.canonicalPath))

      if (notDefined) {
        logger.debug(s"Postpone parsing of ${scalaType} (${pos.source}:${pos.line}:${pos.column}) is not yet compiled")
      }

      if (scalaType.typeSymbol.isParameter || notDefined) {
        logger.debug(s"Skip not fully defined type: ${scalaType}")

        parse(
          tail,
          symtab,
          /*res.*/ examined,
          acceptsType,
          parsed /* ++ res.parsed*/
        )
      } else if (examined contains fullId(scalaType)) {
        // Skip already examined type (or a type parameter)
        logger.debug(s"Skip already examined type: ${scalaType}")

        parse(
          tail,
          symtab,
          /*res.*/ examined,
          acceptsType,
          parsed /* ++ res.parsed*/
        )
      } else {
        val typeArgs = scalaType match {
          case TypeRefTag(t) =>
            t.args

          case _ => List.empty[Type]
        }

        val typeParams: Map[Symbol, Type] =
          scalaType.typeConstructor.typeSymbol.asType.typeParams
            .zip(typeArgs)
            .toMap

        val memberTypes = scalaType.members.collect {
          case MethodSymbolTag(m) if m.isCaseAccessor => {
            val mt = m.typeSignature match {
              case universe.NullaryMethodType(resultType) => // for `=> T`
                resultType

              case t =>
                t
            }

            // Resolve member type according `scalaType` type parameters
            m.name.toString.trim -> typeParams.getOrElse(mt.typeSymbol, mt)
          }

          case TermSymbolTag(t) if (t.isVal) => {
            val tt = t.typeSignature

            t.name.toString.trim -> typeParams.getOrElse(tt.typeSymbol, tt)
          }
        }.toMap

        @annotation.tailrec
        @SuppressWarnings(Array("UnsafeTraversableMethods" /*tail*/ ))
        def walk(
            forest: Seq[Tree],
            syms: Map[String, (Type, Tree)]
          ): List[(Type, Tree)] =
          forest.headOption match {
            case Some(ValOrDefDefTag(tr)) => {
              val k = tr.name.toString.trim

              if (!syms.contains(k)) {
                memberTypes.get(k) match {
                  case Some(tp) =>
                    walk(forest.tail, syms + (k -> (tp -> tr)))

                  case _ =>
                    walk(forest.tail, syms)
                }
              } else {
                walk(forest.tail, syms)
              }
            }

            case Some(tr) =>
              walk(tr.children ++: forest.tail, syms)

            case _ =>
              syms.values.toList
          }

        val members = walk(Seq(tree), Map.empty)
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

  private lazy val enumerationTypeSym: Symbol =
    symbolOf[scala.Enumeration]

  private def parseType(
      tpe: (Type, Tree),
      symtab: Map[String, (Type, Tree)],
      examined: ListSet[TypeFullId],
      acceptsType: Symbol => Boolean
    ): Result[Option, TypeFullId] = {
    val tpeSym = tpe._1.typeSymbol

    import tpe.{ _1 => scalaType }

    if (
      tpeSym.fullName.startsWith("scala.") &&
      !tpeSym.fullName.startsWith("scala.Enumeration.")
    ) {
      logger.debug(s"Skip Scala type: ${tpeSym.fullName}")

      Result(examined + fullId(scalaType), Option.empty[TypeDef])
    } else
      scalaType match {
        case _
            if (tpeSym.isModuleClass &&
              scalaType.baseClasses.contains(enumerationTypeSym)) =>
          parseEnumeration(scalaType, examined)

        case _ if (tpeSym.isModuleClass) =>
          parseObject(tpe, examined)

        case _ if (tpeSym.isClass) => {
          val classSym = tpeSym.asClass

          // TODO: Not sealed trait like CaseClass

          if (
            classSym.isAbstract /*isTrait*/ && classSym.isSealed &&
            scalaType.typeParams.isEmpty
          ) {
            parseSealedUnion(scalaType, symtab, examined, acceptsType)
          } else if (isAnyValChild(scalaType)) {
            parseValueClass(tpe, examined)
          } else if (isCaseClass(scalaType)) {
            parseCaseClass(tpe, examined)
          } else if (isEnumerationValue(scalaType)) {
            val e: Option[Symbol] =
              try {
                Some(
                  mirror.staticModule(fullId(scalaType) stripSuffix ".Value")
                )
              } catch {
                case NonFatal(_) =>
                  None
              }

            e match {
              case Some(enumerationObject) =>
                parseEnumeration(enumerationObject.typeSignature, examined)

              case _ => {
                logger.debug(
                  s"Fails to resolve enumeration object: ${scalaType}"
                )

                Result(examined, Option.empty[TypeDef])
              }
            }
          } else {
            logger.warning(s"Unsupported Scala class: ${tpeSym.fullName}")

            Result(examined + fullId(scalaType), Option.empty[TypeDef])
          }
        }

        case _ => {
          logger.warning(s"Unsupported Scala type: ${tpeSym.fullName}")

          Result(examined + fullId(scalaType), Option.empty[TypeDef])
        }
      }
  }

  private object Field {

    def unapply(m: MethodSymbol): Option[MethodSymbol] =
      if (
        !m.isAbstract && m.isPublic && !m.isImplicit &&
        m.paramLists.forall(_.isEmpty) && {
          val n = m.name.toString
          !(n.contains("$") || n.startsWith("<"))
        } &&
        m.overrides.forall { o =>
          val declaring = o.owner.fullName

          !declaring.startsWith("java.") && !declaring.startsWith("scala.")
        }
      ) {
        Some(m)
      } else {
        None
      }
  }

  private val skipCompanion = true // TODO: (low priority) Configurable

  @inline private def isLiteralType(tpe: Type): Boolean =
    isAnyValChild(tpe) || tpe <:< typeOf[String]

  @annotation.tailrec
  private def appliedOp(
      op: String,
      excludedSymbols: Seq[String],
      application: List[Tree],
      out: List[Tree]
    ): List[Tree] = application match {
    case tree :: tail =>
      tree match {
        case ApplyTag(o) if (o.symbol.name.toString == op) =>
          appliedOp(op, excludedSymbols, o.children ::: tail, out)

        case universe.TypeApplyTag(o) if (o.symbol.name.toString == op) =>
          appliedOp(op, excludedSymbols, o.children ::: tail, out)

        case universe.SelectTag(o) if (o.symbol.name.toString == op) =>
          appliedOp(op, excludedSymbols, o.children ::: tail, out)

        case universe.TypeTreeTag(_) =>
          appliedOp(op, excludedSymbols, tail, out)

        case _ if (excludedSymbols contains tree.symbol.fullName) =>
          appliedOp(op, excludedSymbols, tail, out)

        case _ =>
          appliedOp(op, excludedSymbols, tail, tree :: out)
      }

    case _ =>
      out.reverse

  }

  @SuppressWarnings(Array("UnsafeTraversableMethods" /*tail*/ ))
  private def typeInvariant(
      k: String,
      owner: Tree,
      rhs: Tree,
      hint: Option[ScalaTypeRef]
    ): Option[TypeInvariant] = rhs match {
    case ApplyTag(a)
        if (a.tpe.dealias <:< MapType && a.tpe.typeArgs.size == 2) =>
      a.tpe.typeArgs match {
        case kt :: vt :: Nil =>
          // Dictionary
          val entries: Map[TypeInvariant.Simple, TypeInvariant] =
            a.args.zipWithIndex
              .collect(
                Function.unlift[
                  (Tree, Int),
                  (TypeInvariant.Simple, TypeInvariant)
                ] {
                  case (ArrowedTuple((ky, v)), idx) => {
                    for {
                      key <- simpleTypeInvariant(s"${k}.${idx}", ky, ky, None)

                      vlu <- typeInvariant(s"${k}[${idx}]", v, v, None)
                    } yield key -> vlu
                  }

                  case entry =>
                    logger.warning(s"Unsupported dictionary entry: ${entry}")
                    None
                }
              )
              .toMap

          def scalaTpe(tpe: Type): Option[ScalaTypeRef] = tpe.dealias match {
            case RefinedTypeTag(t) =>
              t.baseClasses.tail.find { cls =>
                val nme = cls.fullName
                !nme.startsWith("scala.") && !nme.startsWith("java.")
              }.map { sym => scalaTypeRef(sym.info, Set.empty) }

            case t => {
              if (t.toString startsWith "scala.") {
                None
              } else {
                Some(scalaTypeRef(t, Set.empty))
              }
            }
          }

          entries.headOption match {
            case Some((fstk, fstv)) =>
              Some(
                DictionaryInvariant(
                  name = k,
                  keyTypeRef = scalaTpe(kt).getOrElse(fstk.typeRef),
                  valueTypeRef = scalaTpe(vt).getOrElse(fstv.typeRef),
                  entries = entries
                )
              )

            case _ => {
              logger.warning(s"Skip empty dictionary: $k")

              None
            }
          }

        case _ =>
          None
      }

    case ApplyTag(a)
        if (a.tpe <:< SeqType && a.symbol.name.toString == "apply" && a.args.nonEmpty) => {
      // Seq/List factory

      scalaTypeRef(a.tpe, Set.empty) match {
        case colTpe @ ListRef(valueTpe) => {
          val elements = a.args.zipWithIndex.collect(
            Function.unlift[(Tree, Int), TypeInvariant] {
              case (e, i) =>
                typeInvariant(s"${k}[${i}]", e, e, None)
            }
          )

          elements.headOption match {
            case Some(_) if (elements.size == a.args.size) =>
              Some(
                ListInvariant(
                  name = k,
                  typeRef = colTpe,
                  valueTypeRef = valueTpe,
                  values = elements
                )
              )

            case _ => {
              logger.warning(s"Skip list with non-stable value: $k")

              None
            }
          }
        }

        case _ =>
          None
      }
    }

    case ApplyTag(a)
        if (a.tpe <:< SeqType && a.symbol.name.toString == f"$$plus$$plus" &&
          a.children.size == 2) =>
      scalaTypeRef(a.tpe, Set.empty) match {
        case colTpe @ ListRef(valueTpe) => {
          val excludes = a.children.tail
            .map(_.symbol.fullName)
            .filter(_ endsWith "canBuildFrom" /* Scala 2.13- */ )

          val terms =
            appliedOp(
              f"$$plus$$plus",
              excludes,
              a.children,
              List.empty
            )

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
              MergedListsInvariant(
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

    case ApplyTag(a)
        if (a.tpe <:< SetType && a.symbol.name.toString == "apply"
          && a.args.nonEmpty) => {
      // Set factory
      scalaTypeRef(a.tpe, Set.empty) match {
        case colTpe @ SetRef(valueTpe) => {
          val elements = a.args.zipWithIndex
            .collect(
              Function.unlift[(Tree, Int), TypeInvariant] {
                case (e, i) =>
                  typeInvariant(s"${k}[${i}]", e, e, None)
              }
            )
            .toSet

          elements.headOption match {
            case Some(_) if (elements.size == a.args.size) =>
              Some(
                SetInvariant(
                  name = k,
                  typeRef = colTpe,
                  valueTypeRef = valueTpe,
                  values = elements
                )
              )

            case _ => {
              logger.warning(s"Skip list with non-stable value: $k")

              None
            }
          }
        }

        case _ =>
          None
      }
    }

    case ApplyTag(a) if (a.tpe <:< SetType && a.children.size == 2) =>
      scalaTypeRef(a.tpe, Set.empty) match {
        case colTpe @ SetRef(valueTpe) => {
          val terms =
            appliedOp(
              f"$$plus$$plus",
              Seq.empty,
              a.children,
              List.empty
            )

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
              MergedSetsInvariant(
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

    case _ =>
      simpleTypeInvariant(k, owner, rhs, hint)

  }

  private def simpleTypeInvariant(
      k: String,
      owner: Tree,
      rhs: Tree,
      hint: Option[ScalaTypeRef]
    ): Option[TypeInvariant.Simple] = rhs match {
    case LiteralTag(v) => {
      // Literal elements in list are not defined with own symbol
      val signature = Option(owner.symbol).map(_.typeSignature)

      def mt: Type =
        signature.map {
          case universe.NullaryMethodType(resultType) => // for `=> T`
            resultType

          case t =>
            t
        }.getOrElse(owner.tpe)

      Some(
        LiteralInvariant(
          name = k,
          typeRef = hint getOrElse scalaTypeRef(mt.dealias, Set.empty),
          value = v.toString
        )
      )
    }

    case ApplyTag(a) if (isLiteralType(a.tpe)) =>
      // Value class
      a.args match {
        case LiteralTag(v) :: Nil =>
          Some(
            LiteralInvariant(
              name = k,
              typeRef = scalaTypeRef(a.tpe.dealias, Set.empty),
              value = v.toString
            )
          )

        case _ =>
          None
      }

    case universe.SelectTag(s) if (s.isTerm && ({
          val sym = s.symbol
          val qual = s.qualifier.symbol

          sym.isPublic &&
          (sym.isTerm ||
            (sym.isMethod &&
              sym.asMethod.paramLists.isEmpty)) &&
          qual.isPublic &&
          (qual.isModule || qual.isModuleClass)
        })) => {
      // Stable reference; e;g. x = qual.y
      val qualTpe = s.qualifier match {
        case universe.ThisTag(_) =>
          ThisTypeRef

        case _ =>
          scalaTypeRef(s.qualifier.tpe.dealias, Set.empty)
      }

      val tpeRef = scalaTypeRef(s.tpe.dealias, Set.empty) match {
        case unknown @ UnknownTypeRef(_) =>
          hint.getOrElse(unknown)

        case tr =>
          tr
      }

      Some(
        SelectInvariant(
          name = k,
          typeRef = tpeRef,
          qualifier = qualTpe,
          term = s.name.toString
        )
      )
    }

    case _ =>
      None
  }

  private lazy val SetType = typeOf[Set[Any]].erasure

  private lazy val SeqType = typeOf[Seq[Any]].erasure

  private lazy val MapType = typeOf[Map[String, Any]].erasure

  @SuppressWarnings(Array("UnsafeTraversableMethods" /*tail*/ ))
  @annotation.tailrec
  private def typeInvariants(
      owner: Type,
      declNames: ListSet[String],
      forest: Seq[Tree],
      vs: List[TypeInvariant]
    ): ListSet[TypeInvariant] =
    forest.headOption match {
      case Some(ValOrDefDefTag(tr)) => {
        val k = tr.name.toString.trim

        if (declNames contains k) {
          typeInvariant(k, tr, tr.rhs, None) match {
            case Some(single) =>
              typeInvariants(
                owner,
                declNames,
                tr.children ++: forest.tail,
                single :: vs
              )

            case _ =>
              typeInvariants(
                owner,
                declNames,
                tr.children ++: forest.tail,
                vs
              )
          }
        } else {
          typeInvariants(owner, declNames, tr.children ++: forest.tail, vs)
        }
      }

      case Some(tr) =>
        typeInvariants(owner, declNames, tr.children ++: forest.tail, vs)

      case _ =>
        ListSet.empty ++ vs.reverse
    }

  private def parseObject(
      tpe: (Type, Tree),
      examined: ListSet[TypeFullId]
    ): Result[Option, TypeFullId] = {
    import tpe.{ _1 => scalaType }

    def classExists: Boolean =
      try {
        Option(mirror staticClass scalaType.typeSymbol.fullName).nonEmpty
      } catch {
        case scala.util.control.NonFatal(_) =>
          false
      }

    if (skipCompanion && classExists) {
      logger.debug(s"Skip companion object: ${scalaType.typeSymbol.fullName}")

      Result(examined + fullId(scalaType), Option.empty[TypeDef])
    } else {
      lazy val declNames: ListSet[String] =
        ListSet.empty ++ scalaType.decls.toList.collect {
          case Field(MethodSymbolTag(m)) => m.name.toString
        }

      @annotation.tailrec
      @SuppressWarnings(Array("UnsafeTraversableMethods" /*tail*/ ))
      def findCtor(trees: Seq[Tree]): Option[Tree] =
        trees.headOption match {
          case Some(t) =>
            t.symbol match {
              case MethodSymbolTag(ctor) if ctor.isConstructor =>
                Some(t)

              case _ =>
                findCtor(t.children ++: trees.tail)
            }

          case _ =>
            None
        }

      lazy val memberNames: Set[String] = scalaType.members.collect {
        case Field(MethodSymbolTag(m)) => m.name.toString
      }.toSet

      @annotation.tailrec
      @SuppressWarnings(Array("UnsafeTraversableMethods" /*tail*/ ))
      def invariants(
          trees: Seq[Tree],
          decls: ListSet[String],
          values: List[TypeInvariant]
        ): (ListSet[String], ListSet[TypeInvariant]) =
        trees.headOption match {
          case Some(ApplyTag(m)) if (m.exists {
                case universe.SuperTag(_) =>
                  true

                case _ =>
                  false
              } && m.symbol.isConstructor) => {
            ((m.symbol.info.paramLists.flatten zip m.args).collectFirst {
              case (s, a) if (memberNames contains s.name.toString) =>
                s -> a
            }) match {
              case Some((s, a)) => {
                val nme = s.name.toString

                invariants(
                  trees.tail,
                  decls - nme,
                  LiteralInvariant(
                    name = nme,
                    typeRef = scalaTypeRef(s.typeSignature, Set.empty),
                    value = a.toString
                  ) :: values
                )
              }

              case _ =>
                invariants(trees.tail, decls, values)
            }
          }

          case Some(t) =>
            invariants(t.children ++: trees.tail, decls, values)

          case _ =>
            decls -> (ListSet.empty[TypeInvariant] ++ values.reverse)
        }

      val (decls, values) = findCtor(Seq(tpe._2)) match {
        case Some(ctor) =>
          invariants(ctor.children, declNames, List.empty)

        case _ =>
          declNames -> ListSet.empty[TypeInvariant]
      }

      val identifier = buildQualifiedIdentifier(scalaType.typeSymbol)

      Result(
        examined = (examined + fullId(scalaType)),
        parsed = Some[TypeDef](
          CaseObject(
            identifier.copy(name = identifier.name stripSuffix ".type"),
            values ++ typeInvariants(tpe._1, decls, Seq(tpe._2), List.empty)
          )
        )
      )
    }
  }

  private def parseSealedUnion(
      tpe: Type,
      symtab: Map[String, (Type, Tree)],
      examined: ListSet[TypeFullId],
      acceptsType: Symbol => Boolean
    ): Result[Option, TypeFullId] = {
    // TODO: (low priority) Check & warn there is no type parameters for a union type

    // Members
    def members = tpe.decls.collect {
      case MethodSymbolTag(m)
          if (m.isAbstract && m.isPublic && !m.isImplicit &&
            !m.name.toString.endsWith("$")) =>
        member(m, List.empty)
    }

    directKnownSubclasses(tpe) match {
      case ps @ (_ :: _) => {
        val possibilities = ps.flatMap { pt => symtab.get(fullId(pt)) }

        if (possibilities.size != ps.size) {
          val pos = tpe.typeSymbol.pos

          logger.info(s"Postpone parsing of sealed union ${tpe.typeSymbol.fullName} (${pos.source}:${pos.line}:${pos.column}) has subclasses are not yet fully defined")

          Result(
            examined = examined,
            parsed = Option.empty[TypeDef]
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
            ListSet.empty[TypeDef]
          )

          Result(
            examined = res.examined + fullId(tpe),
            parsed = Some[TypeDef](
              SealedUnion(
                buildQualifiedIdentifier(tpe.typeSymbol),
                ListSet.empty ++ members,
                res.parsed
              )
            )
          )
        }
      }

      case _ =>
        Result(
          examined = examined + fullId(tpe),
          parsed = Option.empty[TypeDef]
        )
    }
  }

  private def parseEnumeration(
      enumerationType: Type,
      examined: ListSet[TypeFullId]
    ): Result[Option, TypeFullId] = {
    val enumerationObject = enumerationType.typeSymbol
    val identifier = buildQualifiedIdentifier(enumerationObject)

    lazy val enumerationValueSym = enumerationType.member(TypeName("Value"))

    val values = enumerationType.decls.filter { decl =>
      decl.isPublic && decl.isMethod &&
      decl.asMethod.isGetter &&
      decl.asMethod.returnType.typeSymbol == enumerationValueSym

    }.map(_.asTerm.name.toString.trim)

    Result(
      examined = (examined +
        fullId(enumerationValueSym.typeSignature) +
        fullId(enumerationType)),
      parsed = Some[TypeDef](
        EnumerationDef(
          identifier,
          possibilities = ListSet(values.toSeq: _*),
          values = ListSet.empty // TODO: Invariants
        )
      )
    )
  }

  private def parseValueClass(
      tpe: (Type, Tree),
      examined: ListSet[TypeFullId]
    ): Result[Option, TypeFullId] = {
    import tpe.{ _1 => valueClassType }

    val m = valueClassType.members.filter(!_.isMethod).collectFirst {
      case TermSymbolTag(sym) =>
        new TypeMember(
          sym.name.toString.trim,
          scalaTypeRef(sym.info.map(_.dealias), Set.empty)
        )

    }

    m match {
      case Some(vm) =>
        Result(
          examined = examined + fullId(valueClassType),
          parsed = Some[TypeDef](
            ValueClass(buildQualifiedIdentifier(valueClassType.typeSymbol), vm)
          )
        )

      case _ => {
        logger.warning(s"Unsupported Value class: ${valueClassType}")

        Result(
          examined = examined + fullId(valueClassType),
          parsed = Option.empty[TypeDef]
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
      .map(_.name.decodedName.toString)

    val declNames: ListSet[String] =
      ListSet.empty ++ caseClassType.decls.collect {
        case Field(MethodSymbolTag(m)) => m.name.toString
      }

    val values = typeInvariants(tpe._1, declNames, Seq(tpe._2), List.empty)

    // Members
    val members = caseClassType.members.collect {
      case Field(MethodSymbolTag(m))
          if (m.isCaseAccessor && !values.exists(
            _.name == m.name.toString.trim
          )) =>
        m.name.toString.trim -> member(m, typeParams)
    }.toMap

    // Make sure the declaration order is respected
    val orderedMembers: ListSet[TypeMember] =
      ListSet.empty ++ declNames.collect(
        Function.unlift(members.lift)
      ) ++ (members -- declNames).values

    Result(
      examined = examined + fullId(caseClassType),
      parsed = Some[TypeDef](
        CaseClass(
          buildQualifiedIdentifier(caseClassType.typeSymbol),
          orderedMembers,
          ListSet.empty ++ values,
          typeParams
        )
      )
    )
  }

  @inline private def member(
      sym: MethodSymbol,
      typeParams: List[String]
    ) =
    new TypeMember(
      sym.name.toString,
      scalaTypeRef(sym.returnType.map(_.dealias), typeParams.toSet)
    )

  // ---

  private object ArrowedTuple {

    def unapply(tree: Tree): Option[(Tree, Tree)] = tree match {
      case ApplyTag(entry)
          if (entry.tpe.typeSymbol.fullName == "scala.Tuple2") =>
        entry.children match {
          case universe.TypeApplyTag(arrow) :: v :: Nil
              if (arrow.args.size == 1) =>
            arrow.children match {
              case universe.SelectTag(a) :: _ :: Nil
                  if (
                    a.qualifier.tpe.typeSymbol.fullName == "scala.Predef.ArrowAssoc"
                  ) =>
                a.children match {
                  case ApplyTag(ka) :: Nil =>
                    ka.args match {
                      case k :: Nil =>
                        Some(k -> v)

                      case _ =>
                        None
                    }

                  case _ =>
                    None
                }

              case _ =>
                None
            }

          case _ =>
            None
        }

      case _ =>
        None
    }
  }

  private lazy val setSymbol: Symbol =
    mirror.staticClass("_root_.scala.collection.Set")

  private lazy val iterableSymbol: Symbol =
    mirror.staticClass("_root_.scala.collection.Iterable")

  private lazy val optionSymbol: Symbol =
    mirror.staticClass("_root_.scala.Option")

  private lazy val tuple1Symbol: Symbol =
    mirror.staticClass("_root_.scala.Tuple1")

  private[core] def dataTypeRef(scalaType: Type): Option[ScalaTypeRef] =
    scalaType.dealias match {
      case RefinedTypeTag(t) =>
        t.baseClasses.tail.find { cls =>
          val nme = cls.fullName
          !nme.startsWith("scala.") && !nme.startsWith("java.")
        }.map { sym => scalaTypeRef(sym.info, Set.empty) }

      case t => {
        if (t.typeSymbol.fullName startsWith "scala.") {
          None
        } else {
          Some(scalaTypeRef(t, Set.empty))
        }
      }
    }

  private def scalaTypeRef(
      scalaType: Type,
      typeParams: Set[String]
    ): ScalaTypeRef = {
    import scalaType.typeSymbol

    val tpeName: String = typeSymbol.name.toString

    def unknown = UnknownTypeRef(buildQualifiedIdentifier(typeSymbol))

    def nonGenericType = scalaType match {
      case Scalar(ref) =>
        ref

      case _ =>
        tpeName match {
          case typeParam if (typeParams contains typeParam) =>
            TypeParamRef(typeParam)

          case _ => {
            if (isAnyValChild(scalaType)) {
              // #ValueClass_1
              scalaType.members
                .filter(!_.isMethod)
                .map(_.typeSignature)
                .headOption match {
                case Some(valueTpe) =>
                  TaggedRef(
                    identifier = buildQualifiedIdentifier(typeSymbol),
                    tagged = scalaTypeRef(valueTpe, Set.empty)
                  )

                case _ =>
                  unknown
              }
            } else if (isEnumerationValue(scalaType)) {
              val enumerationObject =
                mirror.staticModule(fullId(scalaType) stripSuffix ".Value")

              EnumerationRef(buildQualifiedIdentifier(enumerationObject))
            } else if (typeSymbol.isModuleClass || typeSymbol.isModule) {
              CaseObjectRef(buildQualifiedIdentifier(typeSymbol))
            } else {
              unknown
            }
          }
        }
    }

    scalaType match {
      case TypeRefTag(tpeRef) =>
        if (isCaseClass(scalaType)) {
          val caseClassName = buildQualifiedIdentifier(typeSymbol)
          val typeArgs = tpeRef.args
          val typeArgRefs = typeArgs.map(scalaTypeRef(_, typeParams))

          CaseClassRef(caseClassName, typeArgRefs)
        } else
          (tpeRef.args) match {
            case args @ (a :: b :: _) =>
              tpeName match {
                case "Either" => // TODO: (medium priority) Check type
                  UnionRef(
                    ListSet(
                      scalaTypeRef(a, typeParams),
                      scalaTypeRef(b, typeParams)
                    )
                  )

                case "Map" =>
                  MapRef(
                    scalaTypeRef(a, typeParams),
                    scalaTypeRef(b, typeParams)
                  )

                case _ if (typeSymbol.fullName startsWith "scala.Tuple") =>
                  TupleRef(args.map(a => scalaTypeRef(a, typeParams)))

                case _ =>
                  unknown
              }

            case innerType :: _
                if (scalaType <:< appliedType(optionSymbol, innerType)) =>
              OptionRef(scalaTypeRef(innerType, typeParams))

            case innerType :: _
                if (scalaType <:< appliedType(setSymbol, innerType)) =>
              SetRef(scalaTypeRef(innerType, typeParams))

            case innerType :: _
                if (scalaType <:< appliedType(iterableSymbol, innerType)) =>
              ListRef(scalaTypeRef(innerType, typeParams))

            case innerType :: _
                if (scalaType <:< appliedType(tuple1Symbol, innerType)) =>
              TupleRef(List(scalaTypeRef(innerType, typeParams)))

            case args if (typeSymbol.fullName startsWith "scala.Tuple") =>
              TupleRef(args.map(a => scalaTypeRef(a, typeParams)))

            case _ =>
              nonGenericType

          }

      case _ =>
        nonGenericType
    }
  }

  private object Scalar {

    // TODO: (medium priority) Check type symbol
    def unapply(scalaType: Type): Option[ScalaTypeRef] = {
      val tpeName: String = scalaType.typeSymbol.name.toString

      (scalaType.typeSymbol.fullName -> tpeName) match {
        case (_, "Int" | "Byte" | "Short") =>
          Some(IntRef)

        case (_, "Long") =>
          Some(LongRef)

        case (_, "Float" | "Double") =>
          Some(DoubleRef)

        case (_, "BigDecimal") =>
          Some(BigDecimalRef)

        case (_, "BigInt" | "BigInteger") =>
          Some(BigIntegerRef)

        case (_, "Boolean") =>
          Some(BooleanRef)

        case (_, "String") =>
          Some(StringRef)

        case ("java.util.UUID", _) =>
          Some(UuidRef)

        case ("java.time.LocalDate", _) =>
          Some(DateRef)

        case ("java.time.LocalTime" | "java.time.OffsetTime", _) =>
          Some(TimeRef)

        case (
              full,
              "Instant" | "LocalDateTime" | "ZonedDateTime" | "OffsetDateTime"
            ) if (full startsWith "java.time.") =>
          Some(DateTimeRef)

        case (full, "Date" | "Timestamp") if (full startsWith "java.sql") =>
          Some(DateTimeRef)

        case ("java.util.Date", _) =>
          Some(DateTimeRef)

        case _ =>
          None
      }
    }
  }

  @inline private def isCaseClass(scalaType: Type): Boolean =
    !isAnyValChild(scalaType) &&
      scalaType.typeSymbol.isClass &&
      scalaType.typeSymbol.asClass.isCaseClass &&
      !scalaType.typeSymbol.fullName.startsWith("scala.") /* e.g. Skip Tuple */

  @inline private def isAnyValChild(scalaType: Type): Boolean =
    scalaType <:< typeOf[AnyVal] || scalaType.baseClasses.exists(
      _.fullName == "scala.AnyVal"
    )

  @inline private def isEnumerationValue(scalaType: Type): Boolean = {
    // TODO: (low priority) rather compare Type (than string)
    val sym = scalaType.typeSymbol
    sym.isClass && sym.asClass.fullName == "scala.Enumeration.Value"
  }

  @annotation.tailrec
  private def ownerChain(
      symbol: Symbol,
      acc: List[Symbol] = List.empty
    ): List[Symbol] = {
    if (symbol.owner.isPackage) acc
    else ownerChain(symbol.owner, symbol.owner +: acc)
  }

  @inline private def buildQualifiedIdentifier(
      symbol: Symbol
    ): QualifiedIdentifier = {
    QualifiedIdentifier(
      name = symbol.name.toString,
      enclosingClassNames = ownerChain(symbol).map(_.name.toString)
    )
  }

  private def directKnownSubclasses(tpe: Type): List[Type] = {
    // Workaround for SI-7046: https://issues.scala-lang.org/browse/SI-7046
    val tpeSym = tpe.typeSymbol.asClass

    @SuppressWarnings(Array("UnsafeTraversableMethods" /*tail*/ ))
    @annotation.tailrec
    def allSubclasses(path: Seq[Symbol], subclasses: Set[Type]): Set[Type] =
      path.headOption match {
        case Some(ClassSymbolTag(cls))
            if (tpeSym != cls && cls.selfType.baseClasses.contains(tpeSym)) => {
          val newSub: Set[Type] = if (!cls.isCaseClass) {
            logger.warning(
              s"cannot handle class ${cls.fullName}: no case accessor"
            )
            Set.empty
          } else if (cls.typeParams.nonEmpty) {
            logger.warning(s"cannot handle class ${cls.fullName}: type parameter not supported")
            Set.empty
          } else Set(cls.selfType)

          allSubclasses(path.tail, subclasses ++ newSub)
        }

        case Some(ModuleSymbolTag(o))
            if (o.typeSignature.baseClasses.contains(tpeSym)) =>
          allSubclasses(path.tail, subclasses + o.typeSignature)

        case Some(ModuleSymbolTag(o)) =>
          allSubclasses(o.typeSignature.decls ++: path.tail, subclasses)

        case Some(_) =>
          allSubclasses(path.tail, subclasses)

        case _ =>
          subclasses
      }

    if (tpeSym.isSealed && tpeSym.isAbstract) {
      allSubclasses(tpeSym.owner.typeSignature.decls.toSeq, Set.empty).toList
    } else List.empty
  }

  @inline private def fullId(scalaType: Type): String = {
    if (isEnumerationValue(scalaType)) {
      scalaType.toString
    } else {
      val typeArgs = scalaType match {
        case TypeRefTag(t) =>
          t.args.map(fullId)

        case _ => List.empty[String]
      }

      val n = scalaType.typeSymbol.fullName

      if (typeArgs.isEmpty) n
      else n + typeArgs.mkString("[", ", ", "]")
    }
  }
}

@com.github.ghik.silencer.silent(".*Unused\\ import.*")
private[scalats] object ScalaParser {
  import scala.language.higherKinds

  case class Result[M[_], Tpe](
      examined: ListSet[Tpe],
      parsed: M[TypeDef])

  type TypeFullId = String
}
