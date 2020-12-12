package org.scalats.core

/**
 * Created by Milosz on 09.06.2016.
 */

import scala.collection.immutable.ListSet

import scala.reflect.api.Universe

import com.github.ghik.silencer.silent

final class ScalaParser[U <: Universe](
  universe: U, logger: Logger)(
  implicit
  cu: CompileUniverse[U]) {

  import universe.{
    ClassSymbol,
    MethodSymbol,
    ModuleSymbol,
    NoSymbol,
    NullaryMethodType,
    SingleTypeApi,
    Symbol,
    Type,
    TypeRef,
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

    case _ if tpe.typeSymbol.isClass => {
      val classSym = tpe.typeSymbol.asClass

      if (classSym.isTrait && classSym.isSealed && tpe.typeParams.isEmpty) {
        parseSealedUnion(tpe)
      } else if (isCaseClass(tpe)) {
        parseCaseClass(tpe) // TODO: Special case for ValueClass
      } else if (isEnumerationValue(tpe)) {
        parseEnumeration(tpe)
      } else {
        Option.empty[TypeDef]
      }
    }

    case _ => {
      logger.warning(s"Unsupported Scala type: $tpe")
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

  private val skipCompanion = true // TODO: Configurable

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
      @silent(".*is\\ unchecked.*")
      def members = tpe.decls.collect {
        case Field(m) => member(m, List.empty)
      }

      val identifier = buildQualifiedIdentifier(tpe.typeSymbol)

      Some(CaseObject(
        identifier.copy(name = identifier.name stripSuffix ".type"),
        ListSet.empty ++ members))
    }
  }

  private def parseSealedUnion(tpe: Type): Option[SealedUnion] = {
    // TODO: Check & warn there is no type parameters for a union type

    // Members
    @silent(".*is\\ unchecked.*")
    def members = tpe.decls.collect {
      case m: MethodSymbol if (m.isAbstract && m.isPublic && !m.isImplicit &&
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
    @silent(".*is\\ unchecked.*")
    def members = caseClassType.members.collect {
      case Field(m) if m.isCaseAccessor => member(m, typeParams)
    }.toList

    @silent(".*is\\ unchecked.*")
    def values = caseClassType.decls.collect {
      case Field(m) => member(m, typeParams)
    }.filterNot(members.contains)

    Some(CaseClass(
      buildQualifiedIdentifier(caseClassType.typeSymbol),
      ListSet.empty ++ members,
      ListSet.empty ++ values,
      ListSet.empty ++ typeParams))
  }

  @inline private def member(
    sym: MethodSymbol, typeParams: List[String]) =
    TypeMember(
      sym.name.toString, scalaTypeRef(
        sym.returnType.map(_.dealias), typeParams.toSet))

  @annotation.tailrec
  @silent(".*is\\ unchecked.*")
  private def parse(types: List[Type], examined: ListSet[Type], parsed: ListSet[TypeDef]): ListSet[TypeDef] = types match {
    case scalaType :: tail => {

      if (!examined.contains(scalaType) &&
        !scalaType.typeSymbol.isParameter) {

        val relevantMemberSymbols = scalaType.members.collect {
          case m: MethodSymbol if m.isCaseAccessor => m
        }

        val memberTypes = relevantMemberSymbols.map(
          _.typeSignature.map(_.dealias) match {
            case NullaryMethodType(resultType) => resultType
            case t => t.map(_.dealias)
          })

        val typeArgs = scalaType match {
          case t: TypeRef =>
            t.args

          case _ => List.empty[Type]
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

  // TODO: resolve from implicit (typeclass)
  // TODO: Tuple
  @silent(".*is\\ unchecked.*")
  private def scalaTypeRef(
    scalaType: Type,
    typeParams: Set[String]): ScalaTypeRef = {
    val tpeName = scalaType.typeSymbol.name.toString

    def unknown = UnknownTypeRef(
      buildQualifiedIdentifier(scalaType.typeSymbol))

    def nonGenericType = tpeName match {
      case "Int" | "Byte" | "Short" =>
        IntRef

      case "Long" =>
        LongRef

      case "Double" =>
        DoubleRef

      case "BigDecimal" =>
        BigDecimalRef

      case "Boolean" =>
        BooleanRef

      case "String" =>
        StringRef

      case "UUID" =>
        UuidRef

      case "LocalDate" =>
        DateRef

      case "Instant" | "Timestamp" | "LocalDateTime" | "ZonedDateTime" =>
        DateTimeRef // TODO: OffsetDateTimeb

      case typeParam if typeParams.contains(typeParam) =>
        TypeParamRef(typeParam)

      case _ if isAnyValChild(scalaType) =>
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

    scalaType match {
      case tpeRef: TypeRef => if (isCaseClass(scalaType)) {
        val caseClassName = buildQualifiedIdentifier(scalaType.typeSymbol)
        val typeArgs = tpeRef.args
        val typeArgRefs = typeArgs.map(scalaTypeRef(_, typeParams))

        CaseClassRef(caseClassName, ListSet.empty ++ typeArgRefs)
      } else tpeRef.args match {
        case a :: b :: _ => tpeName match {
          case "Either" =>
            UnionRef(ListSet(
              scalaTypeRef(a, typeParams),
              scalaTypeRef(b, typeParams)))

          case "Map" =>
            MapRef(
              scalaTypeRef(a, typeParams),
              scalaTypeRef(b, typeParams))

          case _ =>
            unknown
        }

        case innerType :: _ => tpeName match {
          case "List" | "Seq" | "Set" => // TODO: Traversable
            SeqRef(scalaTypeRef(innerType, typeParams))

          case "Option" =>
            OptionRef(scalaTypeRef(innerType, typeParams))

          case _ =>
            unknown
        }

        case _ =>
          nonGenericType

      }

      case _ =>
        nonGenericType
    }
  }

  @inline private def isCaseClass(scalaType: Type): Boolean =
    !isAnyValChild(scalaType) &&
      scalaType.typeSymbol.isClass && scalaType.typeSymbol.asClass.isCaseClass

  @inline private def isAnyValChild(scalaType: Type): Boolean =
    scalaType <:< typeOf[AnyVal]

  @inline private def isEnumerationValue(scalaType: Type): Boolean = {
    // FIXME rather compare Type (than string)
    scalaType.typeSymbol.asClass.fullName == "scala.Enumeration.Value"
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
    @silent(".*is\\ unchecked.*")
    def allSubclasses(path: Seq[Symbol], subclasses: Set[Type]): Set[Type] = path.headOption match {
      case Some(cls: ClassSymbol) if (
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

      case Some(o: ModuleSymbol) if (
        o.companion == NoSymbol && // not a companion object
        o.typeSignature.baseClasses.contains(tpeSym)) =>
        allSubclasses(path.tail, subclasses + o.typeSignature)

      case Some(o: ModuleSymbol) if (
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
