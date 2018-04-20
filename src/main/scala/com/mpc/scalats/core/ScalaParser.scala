package com.mpc.scalats.core

/**
  * Created by Milosz on 09.06.2016.
  */

import scala.collection.immutable.ListSet

import scala.reflect.runtime.universe._

// TODO: Keep namespace using fullName from the Type
final class ScalaParser(logger: Logger) {

  import ScalaModel._

  def parseTypes(types: List[Type]): ListSet[TypeDef] =
    parse(types, ListSet.empty[Type], ListSet.empty[TypeDef])

  // ---

  private def parseType(tpe: Type): Option[TypeDef] = tpe match {
    case _: SingleTypeApi =>
      parseObject(tpe)

    case _ if (tpe.getClass.getName contains "ModuleType"/*Workaround*/) =>
      parseObject(tpe)

    case _ if tpe.typeSymbol.isClass => {
      val classSym = tpe.typeSymbol.asClass

      if (classSym.isTrait && classSym.isSealed && tpe.typeParams.isEmpty) {
        parseSealedUnion(tpe)
      } else if (isCaseClass(tpe)) {
        parseCaseClass(tpe)
      } else {
        Option.empty[TypeDef]
      }
    }

    case _ => {
      logger.warning(s"Unsupported Scala type: $tpe")
      Option.empty[TypeDef]
    }
  }

  private def parseObject(tpe: Type): Option[CaseObject] =
    Some(CaseObject(tpe.typeSymbol.name.toString stripSuffix ".type"))

  private def parseSealedUnion(tpe: Type): Option[SealedUnion] =
    directKnownSubclasses(tpe) match {
      case members @ (_ :: _ ) =>
        Some(SealedUnion(tpe.typeSymbol.name.toString, parseTypes(members)))

      case _ => Option.empty[SealedUnion]
    }

  private def parseCaseClass(caseClassType: Type): Option[CaseClass] = {
    val relevantMemberSymbols = caseClassType.members.collect {
      case m: MethodSymbol if m.isCaseAccessor => m
    }

    val typeParams = caseClassType.typeConstructor.normalize match {
      case polyType: PolyTypeApi => polyType.typeParams.map(_.name.decoded)
      case _ => List.empty[String]
    }

    val members = relevantMemberSymbols map { member =>
      CaseClassMember(member.name.toString, getTypeRef(
        member.returnType.map(_.normalize), typeParams.toSet))
    }

    Some(CaseClass(
      caseClassType.typeSymbol.name.toString,
      members.toList,
      typeParams
    ))
  }

  @annotation.tailrec
  private def parse(types: List[Type], examined: ListSet[Type], parsed: ListSet[TypeDef]): ListSet[TypeDef] = types match {
    case scalaType :: tail => {
      if (!examined.contains(scalaType) &&
        !scalaType.typeSymbol.isParameter) {

        val relevantMemberSymbols = scalaType.members.collect {
          case m: MethodSymbol if m.isCaseAccessor => m
        }

        val memberTypes = relevantMemberSymbols.map(
          _.typeSignature.map(_.normalize) match {
            case NullaryMethodType(resultType) => resultType
            case t => t.map(_.normalize)
          })

        val typeArgs = scalaType match {
          case t: scala.reflect.runtime.universe.TypeRef =>
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
  private def getTypeRef(scalaType: Type, typeParams: Set[String]): TypeRef = {
    scalaType.typeSymbol.name.toString match {
      case "Int" | "Byte" | "Short" =>
        IntRef
      case "Long" =>
        LongRef
      case "Double" =>
        DoubleRef
      case "Boolean" =>
        BooleanRef
      case "String" =>
        StringRef
      case "List" | "Seq" | "Set" => // TODO: Traversable
        val innerType = scalaType.asInstanceOf[scala.reflect.runtime.universe.TypeRef].args.head
        SeqRef(getTypeRef(innerType, typeParams))
      case "Option" =>
        val innerType = scalaType.asInstanceOf[scala.reflect.runtime.universe.TypeRef].args.head
        OptionRef(getTypeRef(innerType, typeParams))
      case "LocalDate" =>
        DateRef
      case "Instant" | "Timestamp" | "LocalDateTime" | "ZonedDateTime" =>
        DateTimeRef
      case typeParam if typeParams.contains(typeParam) =>
        TypeParamRef(typeParam)
      case _ if isCaseClass(scalaType) =>
        val caseClassName = scalaType.typeSymbol.name.toString
        val typeArgs = scalaType.asInstanceOf[scala.reflect.runtime.universe.TypeRef].args
        val typeArgRefs = typeArgs.map(getTypeRef(_, typeParams))
        CaseClassRef(caseClassName, typeArgRefs)

      case "Either" => {
        val innerTypeL = scalaType.asInstanceOf[scala.reflect.runtime.universe.TypeRef].args.head
        val innerTypeR = scalaType.asInstanceOf[scala.reflect.runtime.universe.TypeRef].args.last

        UnionRef(ListSet(
          getTypeRef(innerTypeL, typeParams),
          getTypeRef(innerTypeR, typeParams)))
      }

      case "Map" =>
        val keyType = scalaType.asInstanceOf[scala.reflect.runtime.universe.TypeRef].args.head
        val valueType = scalaType.asInstanceOf[scala.reflect.runtime.universe.TypeRef].args.last
        MapRef(getTypeRef(keyType, typeParams), getTypeRef(valueType, typeParams))
      case unknown =>
        //println(s"type ref $typeName umkown")
        UnknownTypeRef(unknown)
    }
  }

  @inline private def isCaseClass(scalaType: Type): Boolean =
    scalaType.typeSymbol.isClass && scalaType.typeSymbol.asClass.isCaseClass

  private def directKnownSubclasses(tpe: Type): List[Type] = {
    // Workaround for SI-7046: https://issues.scala-lang.org/browse/SI-7046
    val tpeSym = tpe.typeSymbol.asClass

    @annotation.tailrec
    def allSubclasses(path: Traversable[Symbol], subclasses: Set[Type]): Set[Type] = path.headOption match {
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
      allSubclasses(tpeSym.owner.typeSignature.decls, Set.empty).toList
    } else List.empty
  }
}
