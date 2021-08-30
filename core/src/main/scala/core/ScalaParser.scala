package io.github.scalats.core

import scala.util.control.NonFatal

import scala.collection.immutable.ListSet

import scala.reflect.api.Universe

final class ScalaParser[Uni <: Universe](
  val universe: Uni,
  compiled: Set[String],
  logger: Logger)(
  implicit
  cu: CompileUniverse[Uni]) {

  import universe.{
    appliedType,
    ClassSymbolTag,
    LiteralTag,
    MethodSymbol,
    MethodSymbolTag,
    ModuleSymbolTag,
    NullaryMethodTypeTag,
    Symbol,
    symbolOf,
    TermSymbolTag,
    Tree,
    Type,
    TypeName,
    TypeRefTag,
    typeOf,
    ValOrDefDefTag
  }

  private lazy val mirror = cu.defaultMirror(universe)

  import ScalaModel.{ TypeRef => ScalaTypeRef, _ }
  import ScalaParser.{ Result, TypeFullId }

  private[scalats] def parseTypes(
    types: List[(Type, Tree)],
    symtab: Map[String, (Type, Tree)],
    examined: ListSet[TypeFullId],
    acceptsType: Symbol => Boolean): Result[ListSet, TypeFullId] =
    parse(types, symtab, examined, acceptsType, ListSet.empty[TypeDef])

  // ---

  @annotation.tailrec
  private def parse(
    types: List[(Type, Tree)],
    symtab: Map[String, (Type, Tree)],
    examined: ListSet[TypeFullId],
    acceptsType: Symbol => Boolean,
    parsed: ListSet[TypeDef]): Result[ListSet, TypeFullId] = types match {
    case ((scalaType, _) :: tail) if (!acceptsType(scalaType.typeSymbol)) => {
      logger.debug(s"Type ${scalaType} is excluded")

      parse(tail, symtab, examined, acceptsType, parsed)
    }

    case (tpe @ (scalaType, tree)) :: tail => {
      val pos = scalaType.typeSymbol.pos
      val notDefined: Boolean = (pos != universe.NoPosition &&
        !compiled.contains(pos.source.file.canonicalPath))

      if (notDefined) {
        logger.info(s"Postpone parsing of ${scalaType} (${pos.source}:${pos.line}:${pos.column}) is not yet compiled")
      }

      if (examined.contains(fullId(scalaType)) ||
        scalaType.typeSymbol.isParameter ||
        notDefined) {
        // Skip already examined type (or a type parameter)
        //val res = parseType(scalaType, examined)

        parse(
          tail,
          symtab,
          /*res.*/ examined,
          acceptsType,
          parsed /* ++ res.parsed*/ )

      } else {
        val typeArgs = scalaType match {
          case TypeRefTag(t) =>
            t.args

          case _ => List.empty[Type]
        }

        val typeParams: Map[Symbol, Type] =
          scalaType.typeConstructor.typeSymbol.
            asType.typeParams.zip(typeArgs).toMap

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
        def walk(
          forest: Seq[Tree],
          syms: Map[String, (Type, Tree)]): List[(Type, Tree)] =
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
          parsed ++ res.parsed)

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
    acceptsType: Symbol => Boolean): Result[Option, TypeFullId] = {
    val tpeSym = tpe._1.typeSymbol

    import tpe.{ _1 => scalaType }

    if (tpeSym.fullName.startsWith("scala.") &&
      !tpeSym.fullName.startsWith("scala.Enumeration.")) {
      logger.debug(s"Skip Scala type: ${tpeSym.fullName}")

      Result(examined + fullId(scalaType), Option.empty[TypeDef])
    } else scalaType match {
      case _ if (tpeSym.isModuleClass &&
        scalaType.baseClasses.contains(enumerationTypeSym)) =>
        parseEnumeration(scalaType, examined)

      case _ if (tpeSym.isModuleClass) =>
        parseObject(tpe, examined)

      case _ if (tpeSym.isClass) => {
        val classSym = tpeSym.asClass

        // TODO: Not sealed trait like CaseClass

        if (classSym.isAbstract /*isTrait*/ && classSym.isSealed &&
          scalaType.typeParams.isEmpty) {
          parseSealedUnion(scalaType, symtab, examined, acceptsType)
        } else if (isAnyValChild(scalaType)) {
          parseValueClass(tpe, examined)
        } else if (isCaseClass(scalaType)) {
          parseCaseClass(tpe, examined)
        } else if (isEnumerationValue(scalaType)) {
          val e: Option[Symbol] = try {
            Some(mirror.staticModule(fullId(scalaType) stripSuffix ".Value"))
          } catch {
            case NonFatal(_) =>
              None
          }

          e match {
            case Some(enumerationObject) =>
              parseEnumeration(enumerationObject.typeSignature, examined)

            case _ =>
              Result(examined, Option.empty[TypeDef])
          }
        } else {
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
      if (!m.isAbstract && m.isPublic && !m.isImplicit &&
        m.paramLists.forall(_.isEmpty) &&
        {
          val n = m.name.toString
          !(n.contains("$") || n.startsWith("<"))
        } &&
        m.overrides.forall { o =>
          val declaring = o.owner.fullName

          !declaring.startsWith("java.") && !declaring.startsWith("scala.")
        }) {
        Some(m)
      } else {
        None
      }
  }

  private val skipCompanion = true // TODO: (low priority) Configurable

  @annotation.tailrec
  private def typeInvariants(
    declNames: Set[String],
    forest: Seq[Tree],
    vs: List[TypeInvariant]): ListSet[TypeInvariant] =
    forest.headOption match {
      case Some(ValOrDefDefTag(tr)) => {
        val k = tr.name.toString.trim

        if (declNames contains k) {
          val mt: Type =
            tr.symbol.typeSignature match {
              case universe.NullaryMethodType(resultType) => // for `=> T`
                resultType

              case t =>
                t
            }

          tr.rhs match {
            case LiteralTag(v) =>
              typeInvariants(
                declNames,
                tr.children ++: forest.tail,
                TypeInvariant(
                  name = k,
                  typeRef = scalaTypeRef(mt.dealias, Set.empty),
                  value = v.toString) :: vs)

            case _ =>
              typeInvariants(declNames, tr.children ++: forest.tail, vs)
          }
        } else {
          typeInvariants(declNames, tr.children ++: forest.tail, vs)
        }
      }

      case Some(tr) =>
        typeInvariants(declNames, tr.children ++: forest.tail, vs)

      case _ =>
        ListSet.empty ++ vs //.reverse
    }

  private def parseObject(
    tpe: (Type, Tree),
    examined: ListSet[TypeFullId]): Result[Option, TypeFullId] = {
    import tpe.{ _1 => scalaType }

    def classExists: Boolean = try {
      Option(mirror staticClass scalaType.typeSymbol.fullName).nonEmpty
    } catch {
      case scala.util.control.NonFatal(_) =>
        false
    }

    if (skipCompanion && classExists) {
      Result(examined + fullId(scalaType), Option.empty[TypeDef])
    } else {
      lazy val declNames: Set[String] = scalaType.decls.collect {
        case Field(MethodSymbolTag(m)) => m.name.toString
      }.toSet

      @annotation.tailrec
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
      def invariants(
        trees: Seq[Tree],
        decls: Set[String],
        values: List[TypeInvariant]): (Set[String], ListSet[TypeInvariant]) =
        trees.headOption match {
          case Some(universe.ApplyTag(m)) if (
            m.exists {
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

                invariants(trees.tail, decls - nme,
                  TypeInvariant(
                    name = nme,
                    typeRef = scalaTypeRef(s.typeSignature, Set.empty),
                    value = a.toString) :: values)
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
        parsed = Some[TypeDef](CaseObject(
          identifier.copy(name = identifier.name stripSuffix ".type"),
          values ++ typeInvariants(decls, Seq(tpe._2), List.empty))))
    }
  }

  private def parseSealedUnion(
    tpe: Type,
    symtab: Map[String, (Type, Tree)],
    examined: ListSet[TypeFullId],
    acceptsType: Symbol => Boolean): Result[Option, TypeFullId] = {
    // TODO: (low priority) Check & warn there is no type parameters for a union type

    // Members
    def members = tpe.decls.collect {
      case MethodSymbolTag(m) if (m.isAbstract && m.isPublic && !m.isImplicit &&
        !m.name.toString.endsWith("$")) => member(m, List.empty)
    }

    directKnownSubclasses(tpe) match {
      case ps @ (_ :: _) => {
        val possibilities = ps.flatMap { pt =>
          symtab.get(fullId(pt))
        }

        val res = parse(
          possibilities,
          symtab,
          examined,
          acceptsType,
          ListSet.empty[TypeDef])

        Result(
          examined = res.examined + fullId(tpe),
          parsed = Some[TypeDef](SealedUnion(
            buildQualifiedIdentifier(tpe.typeSymbol),
            ListSet.empty ++ members,
            res.parsed)))
      }

      case _ =>
        Result(
          examined = examined + fullId(tpe),
          parsed = Option.empty[TypeDef])
    }
  }

  private def parseEnumeration(
    enumerationType: Type,
    examined: ListSet[TypeFullId]): Result[Option, TypeFullId] = {
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
        EnumerationDef(identifier, ListSet(values.toSeq: _*))))
  }

  private def parseValueClass(
    tpe: (Type, Tree),
    examined: ListSet[TypeFullId]): Result[Option, TypeFullId] = {
    import tpe.{ _1 => valueClassType }

    val m = valueClassType.members.filter(!_.isMethod).collectFirst {
      case TermSymbolTag(sym) =>
        new TypeMember(
          sym.name.toString.trim, scalaTypeRef(
            sym.info.map(_.dealias), Set.empty))

    }

    m match {
      case Some(vm) =>
        Result(
          examined = examined + fullId(valueClassType),
          parsed = Some[TypeDef](ValueClass(
            buildQualifiedIdentifier(valueClassType.typeSymbol), vm)))

      case _ => {
        logger.warning(s"Unsupported Value class: ${valueClassType}")

        Result(
          examined = examined + fullId(valueClassType),
          parsed = Option.empty[TypeDef])
      }
    }
  }

  // TODO: Parse default field values
  private def parseCaseClass(
    tpe: (Type, Tree),
    examined: ListSet[TypeFullId]): Result[Option, TypeFullId] = {
    import tpe.{ _1 => caseClassType }

    val typeParams = caseClassType.typeConstructor.
      dealias.typeParams.map(_.name.decodedName.toString)

    lazy val declNames: Set[String] = caseClassType.decls.collect {
      case Field(MethodSymbolTag(m)) => m.name.toString
    }.toSet

    val values = typeInvariants(declNames, Seq(tpe._2), List.empty)

    // Members
    def members = caseClassType.members.collect {
      case Field(MethodSymbolTag(m)) if (
        m.isCaseAccessor && !values.exists(_.name == m.name.toString.trim)) =>
        member(m, typeParams)
    }.toList

    Result(
      examined = examined + fullId(caseClassType),
      parsed = Some[TypeDef](CaseClass(
        buildQualifiedIdentifier(caseClassType.typeSymbol),
        ListSet.empty ++ members,
        ListSet.empty ++ values,
        typeParams)))
  }

  @inline private def member(
    sym: MethodSymbol, typeParams: List[String]) =
    new TypeMember(
      sym.name.toString, scalaTypeRef(
        sym.returnType.map(_.dealias), typeParams.toSet))

  // ---

  private lazy val iterableSymbol: Symbol =
    mirror.staticClass("_root_.scala.collection.Iterable")

  private lazy val optionSymbol: Symbol =
    mirror.staticClass("_root_.scala.Option")

  private lazy val tuple1Symbol: Symbol =
    mirror.staticClass("_root_.scala.Tuple1")

  private def scalaTypeRef(
    scalaType: Type,
    typeParams: Set[String]): ScalaTypeRef = {
    import scalaType.typeSymbol
    val tpeName: String = typeSymbol.name.toString

    def unknown = UnknownTypeRef(
      buildQualifiedIdentifier(typeSymbol))

    def nonGenericType = scalaType match {
      case Scalar(ref) =>
        ref

      case _ => tpeName match {
        case typeParam if (typeParams contains typeParam) =>
          TypeParamRef(typeParam)

        case _ if isAnyValChild(scalaType) =>
          // #ValueClass_1
          scalaType.members.filter(!_.isMethod).
            map(_.typeSignature).headOption match {
              case Some(valueTpe) =>
                TaggedRef(
                  identifier = buildQualifiedIdentifier(typeSymbol),
                  tagged = scalaTypeRef(valueTpe, Set.empty))

              case _ =>
                unknown
            }

        case _ if isEnumerationValue(scalaType) => {
          val enumerationObject = mirror.staticModule(
            fullId(scalaType) stripSuffix ".Value")

          EnumerationRef(buildQualifiedIdentifier(enumerationObject))
        }

        case _ =>
          unknown
      }
    }

    scalaType match {
      case TypeRefTag(tpeRef) => if (isCaseClass(scalaType)) {
        val caseClassName = buildQualifiedIdentifier(typeSymbol)
        val typeArgs = tpeRef.args
        val typeArgRefs = typeArgs.map(scalaTypeRef(_, typeParams))

        CaseClassRef(caseClassName, typeArgRefs)
      } else (tpeRef.args) match {
        case args @ (a :: b :: _) => tpeName match {
          case "Either" => // TODO: (medium priority) Check type
            UnionRef(ListSet(
              scalaTypeRef(a, typeParams),
              scalaTypeRef(b, typeParams)))

          case "Map" =>
            MapRef(
              scalaTypeRef(a, typeParams),
              scalaTypeRef(b, typeParams))

          case _ if (typeSymbol.fullName startsWith "scala.Tuple") =>
            TupleRef(args.map(a => scalaTypeRef(a, typeParams)))

          case _ =>
            unknown
        }

        case innerType :: _ if (
          scalaType <:< appliedType(optionSymbol, innerType)) =>
          OptionRef(scalaTypeRef(innerType, typeParams))

        case innerType :: _ if (
          scalaType <:< appliedType(iterableSymbol, innerType)) =>
          CollectionRef(scalaTypeRef(innerType, typeParams))

        case innerType :: _ if (
          scalaType <:< appliedType(tuple1Symbol, innerType)) =>
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

        case (full, "Instant" | "LocalDateTime" | "ZonedDateTime" | "OffsetDateTime") if (full startsWith "java.time.") =>
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
      _.fullName == "scala.AnyVal")

  @inline private def isEnumerationValue(scalaType: Type): Boolean = {
    // TODO: (low priority) rather compare Type (than string)
    val sym = scalaType.typeSymbol
    sym.isClass && sym.asClass.fullName == "scala.Enumeration.Value"
  }

  @annotation.tailrec
  private def ownerChain(symbol: Symbol, acc: List[Symbol] = List.empty): List[Symbol] = {
    if (symbol.owner.isPackage) acc
    else ownerChain(symbol.owner, symbol.owner +: acc)
  }

  @inline private def buildQualifiedIdentifier(symbol: Symbol): QualifiedIdentifier = {
    QualifiedIdentifier(
      name = symbol.name.toString,
      enclosingClassNames = ownerChain(symbol).map(_.name.toString))
  }

  private def directKnownSubclasses(tpe: Type): List[Type] = {
    // Workaround for SI-7046: https://issues.scala-lang.org/browse/SI-7046
    val tpeSym = tpe.typeSymbol.asClass

    @annotation.tailrec
    def allSubclasses(path: Seq[Symbol], subclasses: Set[Type]): Set[Type] =
      path.headOption match {
        case Some(ClassSymbolTag(cls)) if (
          tpeSym != cls && cls.selfType.baseClasses.contains(tpeSym)) => {
          val newSub: Set[Type] = if (!cls.isCaseClass) {
            logger.warning(s"cannot handle class ${cls.fullName}: no case accessor")
            Set.empty
          } else if (cls.typeParams.nonEmpty) {
            logger.warning(s"cannot handle class ${cls.fullName}: type parameter not supported")
            Set.empty
          } else Set(cls.selfType)

          allSubclasses(path.tail, subclasses ++ newSub)
        }

        case Some(ModuleSymbolTag(o)) if (
          o.typeSignature.baseClasses.contains(tpeSym)) =>
          allSubclasses(path.tail, subclasses + o.typeSignature)

        case Some(ModuleSymbolTag(o)) =>
          allSubclasses(
            o.typeSignature.decls ++: path.tail,
            subclasses)

        case Some(_) =>
          allSubclasses(path.tail, subclasses)

        case _ => subclasses
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
    parsed: M[ScalaModel.TypeDef])

  type TypeFullId = String
}
