package io.github.scalats.core

/**
 * Created by Milosz on 09.06.2016.
 */

import scala.collection.immutable.ListSet

import scala.reflect.api.Universe

final class ScalaParser[Uni <: Universe](
  val universe: Uni, logger: Logger)(
  implicit
  cu: CompileUniverse[Uni]) {

  import universe.{
    ClassSymbolTag,
    MethodSymbol,
    MethodSymbolTag,
    ModuleSymbolTag,
    NoSymbol,
    NullaryMethodTypeTag,
    Symbol,
    symbolOf,
    Type,
    TypeName,
    TypeRefTag,
    typeOf
  }

  private lazy val mirror = cu.defaultMirror(universe)

  import ScalaModel.{ TypeRef => ScalaTypeRef, _ }
  import ScalaParser.{ Result, TypeFullId }

  def parseTypes(types: List[Type]): Result[ListSet, TypeFullId] =
    parse(types, ListSet.empty[TypeFullId], ListSet.empty[TypeDef])

  private[scalats] def parseTypes(
    types: List[Type],
    examined: ListSet[TypeFullId]): Result[ListSet, TypeFullId] =
    parse(types, examined, ListSet.empty[TypeDef])

  // ---

  @annotation.tailrec
  private def parse(
    types: List[Type],
    examined: ListSet[TypeFullId],
    parsed: ListSet[TypeDef]): Result[ListSet, TypeFullId] = types match {
    case scalaType :: tail => {
      if (examined.contains(fullId(scalaType)) ||
        scalaType.typeSymbol.isParameter) {
        // Skip already examined type (or a type parameter)
        //val res = parseType(scalaType, examined)

        parse(
          tail,
          /*res.*/ examined,
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
          case MethodSymbolTag(m) if m.isCaseAccessor =>
            val mt = m.typeSignature match {
              case universe.NullaryMethodType(resultType) => // for `=> T`
                resultType

              case t =>
                t
            }

            // Resolve member type according `scalaType` type parameters
            typeParams.getOrElse(mt.typeSymbol, mt)
        }

        val res = parseType(scalaType, examined)

        parse(
          memberTypes ++: typeArgs ++: tail,
          res.examined,
          parsed ++ res.parsed)

      }
    }

    case _ =>
      Result(examined, parsed)
  }

  private lazy val enumerationTypeSym: Symbol =
    symbolOf[scala.Enumeration]

  private def parseType(
    tpe: Type,
    examined: ListSet[TypeFullId]): Result[Option, TypeFullId] = {
    val tpeSym = tpe.typeSymbol

    tpe match {
      case _ if (tpeSym.isModuleClass &&
        tpe.baseClasses.contains(enumerationTypeSym)) =>
        parseEnumeration(tpe, examined)

      case _ if (tpeSym.isModuleClass &&
        !tpeSym.fullName.startsWith("scala.")) =>
        parseObject(tpe, examined)

      case _ if (tpeSym.isClass) => {
        // TODO: Special case for ValueClass; See #ValueClass_1

        val classSym = tpeSym.asClass

        if (classSym.isTrait && classSym.isSealed && tpe.typeParams.isEmpty) {
          parseSealedUnion(tpe, examined)
        } else if (isCaseClass(tpe)) {
          parseCaseClass(tpe, examined)
        } else if (isEnumerationValue(tpe)) {
          val e: Option[Symbol] = try {
            Some(mirror.staticModule(fullId(tpe) stripSuffix ".Value"))
          } catch {
            case scala.util.control.NonFatal(_) =>
              None
          }

          e match {
            case Some(enumerationObject) =>
              parseEnumeration(enumerationObject.typeSignature, examined)

            case _ =>
              Result(examined, Option.empty[TypeDef])
          }
        } else {
          Result(examined + fullId(tpe), Option.empty[TypeDef])
        }
      }

      case _ => {
        logger.warning(s"Unsupported Scala type: ${tpeSym.fullName}")

        Result(examined + fullId(tpe), Option.empty[TypeDef])
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

  private def parseObject(
    tpe: Type,
    examined: ListSet[TypeFullId]): Result[Option, TypeFullId] = {
    def classExists: Boolean = try {
      Option(mirror staticClass tpe.typeSymbol.fullName).nonEmpty
    } catch {
      case scala.util.control.NonFatal(_) =>
        false
    }

    if (skipCompanion && classExists) {
      Result(examined + fullId(tpe), Option.empty[TypeDef])
    } else {
      def members = tpe.decls.collect {
        case Field(MethodSymbolTag(m)) =>
          member(m, List.empty)
      }

      val identifier = buildQualifiedIdentifier(tpe.typeSymbol)

      Result(
        examined = (examined + fullId(tpe)),
        parsed = Some[TypeDef](CaseObject(
          identifier.copy(name = identifier.name stripSuffix ".type"),
          ListSet.empty ++ members)))
    }
  }

  private def parseSealedUnion(
    tpe: Type,
    examined: ListSet[TypeFullId]): Result[Option, TypeFullId] = {
    // TODO: (low priority) Check & warn there is no type parameters for a union type

    // Members
    def members = tpe.decls.collect {
      case MethodSymbolTag(m) if (m.isAbstract && m.isPublic && !m.isImplicit &&
        !m.name.toString.endsWith("$")) => member(m, List.empty)
    }

    directKnownSubclasses(tpe) match {
      case possibilities @ (_ :: _) => {
        val res = parse(
          possibilities,
          examined,
          ListSet.empty[TypeDef])

        Result(
          examined = res.examined + fullId(tpe),
          parsed = Some[TypeDef](SealedUnion(
            buildQualifiedIdentifier(tpe.typeSymbol),
            ListSet.empty ++ members,
            res.parsed)))
      }

      case _ => Result(
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

  private def parseCaseClass(
    caseClassType: Type,
    examined: ListSet[TypeFullId]): Result[Option, TypeFullId] = {
    val typeParams = caseClassType.typeConstructor.
      dealias.typeParams.map(_.name.decodedName.toString)

    // Members
    def members = caseClassType.members.collect {
      case Field(MethodSymbolTag(m)) if m.isCaseAccessor =>
        member(m, typeParams)
    }.toList

    def values = caseClassType.decls.collect {
      case Field(MethodSymbolTag(m)) => member(m, typeParams)
    }.filterNot(members.contains)

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
    TypeMember(
      sym.name.toString, scalaTypeRef(
        sym.returnType.map(_.dealias), typeParams.toSet))

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
                scalaTypeRef(valueTpe, Set.empty)

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
          case "Either" =>
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

        case innerType :: _ => tpeName match {
          case "List" | "Seq" | "Set" =>
            //println(s"?$innerType---> ${universe.appliedType(mirror.staticClass("scala.collection.Traversable"), innerType)}")
            // TODO: (medium priority) Rather check type is Traversable
            SeqRef(scalaTypeRef(innerType, typeParams))

          case "Option" =>
            OptionRef(scalaTypeRef(innerType, typeParams))

          case "Tuple1" if (typeSymbol.fullName startsWith "scala.") =>
            TupleRef(List(scalaTypeRef(innerType, typeParams)))

          case _ =>
            unknown
        }

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
    scalaType <:< typeOf[AnyVal]

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
    def allSubclasses(path: Seq[Symbol], subclasses: Set[Type]): Set[Type] = path.headOption match {
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
        o.companion == NoSymbol && // not a companion object
        o.typeSignature.baseClasses.contains(tpeSym)) =>
        allSubclasses(path.tail, subclasses + o.typeSignature)

      case Some(ModuleSymbolTag(o)) if (
        o.companion == NoSymbol // not a companion object
      ) => allSubclasses(path.tail, subclasses)

      case Some(_) => allSubclasses(path.tail, subclasses)

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
