package com.mpc.scalats.core

/**
  * Created by Milosz on 09.06.2016.
  */
import java.time.{Instant, LocalDate}

import scala.reflect.runtime.universe._

object ScalaParser {

  import ScalaModel._

  def parseCaseClasses(caseClassTypes: List[Type]): Map[String, CaseClass] =
    parseCaseClasses(caseClassTypes, Map.empty, Set.empty)

  def parseCaseClass(caseClassType: Type): Map[String, CaseClass] =
    parseCaseClass(caseClassType, Map.empty, Set.empty)

  def parseCaseClass(caseClassType: Type,
                     declarations: Map[String, CaseClass],
                     typesBeingResolved: Set[String]): Map[String, CaseClass] = {
    val relevantMembers = caseClassType.members.collect {
      case m: MethodSymbol if m.isCaseAccessor => m
    }
    val caseClassMembers = relevantMembers map { member =>
      val memberName = member.name.toString
      val returnType = member.returnType
      CaseClassMember(memberName, getTypeRef(member.returnType))
    }

    val referencedTypes = caseClassMembers flatMap { case CaseClassMember(_, typeRef) =>
      getReferencedType(typeRef)
    }

    val updatedDeclarations = parseCaseClasses(referencedTypes.toList, declarations, typesBeingResolved)
    val caseClass = CaseClass(caseClassType.typeSymbol.name.toString, caseClassMembers.toList)
    Map(caseClassType.typeSymbol.name.toString -> caseClass) ++ updatedDeclarations
  }

  def parseCaseClasses(caseClassTypes: List[Type],
                       declarations: Map[String, CaseClass],
                       typesBeingResolved: Set[String]): Map[String, CaseClass] = {
    caseClassTypes.foldLeft(declarations)({ case (acc, caseClassType) =>
      val referencedTypeName = caseClassType.typeSymbol.name.toString
      if (declarations.contains(referencedTypeName) || typesBeingResolved.contains(referencedTypeName)) acc
      else parseCaseClass(caseClassType, acc, typesBeingResolved + referencedTypeName) ++ acc
    })
  }

  def getTypeRef(scalaType: Type): TypeRef = {
    val typeName = scalaType.typeSymbol.name.toString
    if (typeName == "Int") {
      IntRef
    } else if (typeName == "String") {
      StringRef
    } else if (Set("List", "Seq").contains(typeName)) {
      val innerType = scalaType.asInstanceOf[scala.reflect.runtime.universe.TypeRef].args.head
      SeqRef(getTypeRef(innerType))
    } else if (typeName == "LocalDate") {
      DateRef
    } else if (typeName == "Instant") {
      DateTimeRef
    } else {
      val caseClassName = scalaType.typeSymbol.name.toString
      CaseClassRef(caseClassName, scalaType)
    }
  }

  def getReferencedType(typeRef: TypeRef): Option[Type] = typeRef match {
    case CaseClassRef(name, t) => Some(t)
    case SeqRef(innerType) => getReferencedType(innerType)
    case _ => None
  }


}
