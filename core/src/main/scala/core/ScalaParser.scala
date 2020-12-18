package io.github.scalats.core

/**
 * Created by Milosz on 09.06.2016.
 */

import scala.collection.immutable.ListSet

import scala.reflect.api.Universe

final class ScalaParser[U <: Universe](
  universe: U, logger: Logger)(
  implicit
  cu: CompileUniverse[U]) {

  import universe.{
    ClassSymbolTag,
    MethodSymbol,
    MethodSymbolTag,
    ModuleSymbolTag,
    NoSymbol,
    NullaryMethodTypeTag,
    SingleTypeApi,
    Symbol,
    Type,
    TypeRefTag,
    typeOf
  }

  private lazy val mirror = cu.defaultMirror(universe)

  import ScalaModel.{ TypeRef => ScalaTypeRef, _ }

  def parseTypes(types: List[Type]): ListSet[TypeDef] =
    parse(types, ListSet.empty[Type], ListSet.empty[TypeDef])

  // ---

  private def parseType(tpe: Type): Option[TypeDef] = tpe match {
    case _: SingleTypeApi =>
      parseObject(tpe)

    case _ if (tpe.getClass.getName contains "ModuleType" /*Workaround*/ ) =>
      parseObject(tpe)

    case _ if (tpe.typeSymbol.isClass) => {
      // TODO: Special case for ValueClass; See #ValueClass_1

      val classSym = tpe.typeSymbol.asClass

      if (classSym.isTrait && classSym.isSealed && tpe.typeParams.isEmpty) {
        parseSealedUnion(tpe)
      } else if (isCaseClass(tpe)) {
        parseCaseClass(tpe)
      } else if (isEnumerationValue(tpe)) {
        parseEnumeration(tpe)
      } else {
        Option.empty[TypeDef]
      }
    }

    case _ => {
      logger.warning(s"Unsupported Scala type: ${tpe.typeSymbol.fullName}")
      Option.empty[TypeDef]
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

  private def parseObject(tpe: Type): Option[CaseObject] = {
    def classExists: Boolean = try {
      Option(mirror staticClass tpe.typeSymbol.fullName).nonEmpty
    } catch {
      case scala.util.control.NonFatal(_) =>
        false
    }

    if (skipCompanion && classExists) {
      Option.empty
    } else {
      def members = tpe.decls.collect {
        case Field(MethodSymbolTag(m)) => member(m, List.empty)
      }

      val identifier = buildQualifiedIdentifier(tpe.typeSymbol)

      Some(CaseObject(
        identifier.copy(name = identifier.name stripSuffix ".type"),
        ListSet.empty ++ members))
    }
  }

  private def parseSealedUnion(tpe: Type): Option[SealedUnion] = {
    // TODO: (low priority) Check & warn there is no type parameters for a union type

    // Members
    def members = tpe.decls.collect {
      case MethodSymbolTag(m) if (m.isAbstract && m.isPublic && !m.isImplicit &&
        !m.name.toString.endsWith("$")) => member(m, List.empty)
    }

    directKnownSubclasses(tpe) match {
      case possibilities @ (_ :: _) =>
        Some(SealedUnion(
          buildQualifiedIdentifier(tpe.typeSymbol),
          ListSet.empty ++ members,
          parseTypes(possibilities)))

      case _ => Option.empty[SealedUnion]
    }
  }

  private def parseEnumeration(enumerationValueType: Type): Option[Enumeration] = {
    val enumerationObject = enumerationValueType.typeSymbol.owner
    val identifier = buildQualifiedIdentifier(enumerationObject)

    val values = enumerationObject.info.decls.filter { decl =>
      decl.isPublic && decl.isMethod &&
        decl.asMethod.isGetter &&
        decl.asMethod.returnType =:= enumerationValueType
    }.map(_.asTerm.name.toString.trim)

    Some(Enumeration(identifier, ListSet(values.toSeq: _*)))
  }

  private def parseCaseClass(caseClassType: Type): Option[CaseClass] = {
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

    Some(CaseClass(
      buildQualifiedIdentifier(caseClassType.typeSymbol),
      ListSet.empty ++ members,
      ListSet.empty ++ values,
      typeParams))
  }

  @inline private def member(
    sym: MethodSymbol, typeParams: List[String]) =
    TypeMember(
      sym.name.toString, scalaTypeRef(
        sym.returnType.map(_.dealias), typeParams.toSet))

  @annotation.tailrec
  private def parse(
    types: List[Type],
    examined: ListSet[Type],
    parsed: ListSet[TypeDef]): ListSet[TypeDef] = types match {
    case scalaType :: tail => {
      if (!examined.contains(scalaType) &&
        !scalaType.typeSymbol.isParameter) {

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

        parse(
          memberTypes ++: typeArgs ++: tail,
          examined + scalaType,
          parsed ++ parseType(scalaType))

      } else {
        parse(tail, examined + scalaType, parsed ++ parseType(scalaType))
      }
    }

    case _ => parsed
  }

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
            scalaType.toString stripSuffix ".Value")

          EnumerationRef(
            identifier = buildQualifiedIdentifier(enumerationObject))
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

        case (_, "Double") =>
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
}
