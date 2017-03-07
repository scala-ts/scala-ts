package com.mpc.scalats.core

/**
  * Created by Milosz on 09.06.2016.
  */

import scala.reflect.runtime.universe._

object ScalaParser {

  import ScalaModel._

  def parseCaseClasses(classTypes: List[Type]): List[Entity] = {
    val involvedTypes = classTypes flatMap getInvolvedTypes(Set.empty)
    val typesToParse = (involvedTypes filter isEntityType).distinct
    (typesToParse map parseType).distinct
  }

  private def parseType(aType: Type) = {
    val relevantMemberSymbols = aType.members.collect {
      case m: MethodSymbol if m.isAccessor => m
    }
    val typeParams = aType.typeConstructor.dealias.etaExpand match {
      case polyType: PolyTypeApi => polyType.typeParams.map(_.name.decodedName.toString)
      case _ => List.empty[String]
    }
    val members = relevantMemberSymbols map { member =>
      val memberName = member.name.toString
      EntityMember(memberName, getTypeRef(member.returnType.map(_.normalize), typeParams.toSet))
    }
    Entity(
      aType.typeSymbol.name.toString,
      members.toList,
      typeParams
    )
  }

  private def getInvolvedTypes(alreadyExamined: Set[Type])(scalaType: Type): List[Type] = {
    if (!alreadyExamined.contains(scalaType) && !scalaType.typeSymbol.isParameter) {
      val relevantMemberSymbols = scalaType.members.collect {
        case m: MethodSymbol if m.isCaseAccessor => m
      }
      val memberTypes = relevantMemberSymbols.map(_.typeSignature.map(_.normalize) match {
        case NullaryMethodType(resultType) => resultType
        case t => t.map(_.normalize)
      }).flatMap(getInvolvedTypes(alreadyExamined + scalaType))
      val typeArgs = scalaType match {
        case t: scala.reflect.runtime.universe.TypeRef => t.args.flatMap(getInvolvedTypes(alreadyExamined + scalaType))
        case _ => List.empty
      }
      (scalaType.typeConstructor :: typeArgs ::: memberTypes.toList).filter(!_.typeSymbol.isParameter).distinct
    } else {
      List.empty
    }
  }

  private def getTypeRef(scalaType: Type, typeParams: Set[String]): TypeRef = {
    val typeName = scalaType.typeSymbol.name.toString
    typeName match {
      case "Int" | "Byte" =>
        IntRef
      case "Long" =>
        LongRef
      case "Double" =>
        DoubleRef
      case "Boolean" =>
        BooleanRef
      case "String" =>
        StringRef
      case "List" | "Seq" | "Set" =>
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
      case _ if isEntityType(scalaType) =>
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
      case "Value" =>
        IntRef
      case _ =>
        //println(s"type ref $typeName umkown")
        UnknownTypeRef(typeName)
    }
  }

  private def isNotScalaCollectionMember(classSymbol: ClassSymbol) =
    !classSymbol.fullName.startsWith("scala.collection.")

  private def isEntityType(scalaType: Type) = {
    val typeSymbol = scalaType.typeSymbol
    if (typeSymbol.isClass) {
      val classSymbol = typeSymbol.asClass
      isNotScalaCollectionMember(classSymbol) && (classSymbol.isCaseClass || classSymbol.isTrait)
    }
    else false
  }

}
