package com.mpc.scalats.core

/**
  * Created by Milosz on 09.06.2016.
  */

import scala.collection.immutable.ListSet

import scala.reflect.runtime.universe._

object ScalaParser {

  import ScalaModel._

  def parseCaseClasses(types: List[Type]): ListSet[TypeDef] =
    parse(types, ListSet.empty[Type], ListSet.empty[TypeDef])

  // ---

  private def parseType(tpe: Type): Option[TypeDef] = tpe match {
    case _: SingleTypeApi =>
      Some(CaseObject(tpe.typeSymbol.name.toString))

    case _ if isCaseClass(tpe) => Some(parseCaseClass(tpe))
    case _ => Option.empty
  }

  private def parseCaseClass(caseClassType: Type): CaseClass = {
    val relevantMemberSymbols = caseClassType.members.collect {
      case m: MethodSymbol if m.isCaseAccessor => m
    }

    val typeParams = caseClassType.typeConstructor.normalize match {
      case polyType: PolyTypeApi => polyType.typeParams.map(_.name.decoded)
      case _ => List.empty[String]
    }

    val members = relevantMemberSymbols map { member =>
      val memberName = member.name.toString
      CaseClassMember(memberName, getTypeRef(member.returnType.map(_.normalize), typeParams.toSet))
    }

    CaseClass(
      caseClassType.typeSymbol.name.toString,
      members.toList,
      typeParams
    )
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
      case "Either" =>
        val innerTypeL = scalaType.asInstanceOf[scala.reflect.runtime.universe.TypeRef].args.head
        val innerTypeR = scalaType.asInstanceOf[scala.reflect.runtime.universe.TypeRef].args.last
        UnionRef(getTypeRef(innerTypeL, typeParams), getTypeRef(innerTypeR, typeParams))
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
}
