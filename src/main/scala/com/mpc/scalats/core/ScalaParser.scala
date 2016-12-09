package com.mpc.scalats.core

/**
  * Created by Milosz on 09.06.2016.
  */
import scala.reflect.runtime.universe._

object ScalaParser {

  import ScalaModel._

  def parseCaseClasses(caseClassTypes: List[Type]): Map[String, CaseClass] =
    parseCaseClassesImpl(caseClassTypes, Map.empty, Set.empty)

  private def parseCaseClass(
                              caseClassType: Type,
                              declarations: Map[String, CaseClass],
                              typesBeingResolved: Set[String]
                            ): Map[String, CaseClass] = {
    val relevantMemberSymbols = caseClassType.members.collect {
      case m: MethodSymbol if m.isCaseAccessor => m
    }

    val typeParams = caseClassType.typeConstructor.normalize match {
      case polyType: PolyTypeApi =>
        polyType.typeParams.map(_.name.decoded)
      case _ =>
        List.empty[String]
    }

    val members = relevantMemberSymbols map { member =>
      val memberName = member.name.toString
      CaseClassMember(memberName, getTypeRef(member.returnType, typeParams.toSet))
    }

    val referencedTypes = members flatMap { case CaseClassMember(_, typeRef) =>
      getReferencedCaseClasses(typeRef)
    }

    val updatedDeclarations = parseCaseClassesImpl(referencedTypes.toList, declarations, typesBeingResolved)
    val caseClass = CaseClass(
      caseClassType.typeSymbol.name.toString,
      members.toList,
      typeParams
    )
    Map(caseClassType.typeSymbol.name.toString -> caseClass) ++ updatedDeclarations
  }

  private def parseCaseClassesImpl(
                                    caseClassTypes: List[Type],
                                    declarations: Map[String, CaseClass],
                                    typesBeingResolved: Set[String]
                                  ): Map[String, CaseClass] = {
    caseClassTypes.foldLeft(declarations)({ case (acc, caseClassType) =>
      val referencedTypeName = caseClassType.typeSymbol.name.toString
      if (declarations.contains(referencedTypeName) || typesBeingResolved.contains(referencedTypeName)) acc
      else parseCaseClass(caseClassType, acc, typesBeingResolved + referencedTypeName) ++ acc
    })
  }

  private def getTypeRef(scalaType: Type, typeParams: Set[String]): TypeRef = {
    val typeName = scalaType.typeSymbol.name.toString
    val isCaseClass = scalaType.members.collect({ case m: MethodSymbol if m.isCaseAccessor => m }).nonEmpty
    typeName match {
      case "Int" => IntRef
      case "Double" => DoubleRef
      case "Boolean" => BooleanRef
      case "String" => StringRef
      case "List" | "Seq" =>
        val innerType = scalaType.asInstanceOf[scala.reflect.runtime.universe.TypeRef].args.head
        SeqRef(getTypeRef(innerType, typeParams))
      case "Option" =>
        val innerType = scalaType.asInstanceOf[scala.reflect.runtime.universe.TypeRef].args.head
        OptionRef(getTypeRef(innerType, typeParams))
      case "LocalDate" => DateRef
      case "Instant" => DateTimeRef
      case typeParam if typeParams.contains(typeParam) => TypeParamRef(typeParam)
      case _ if isCaseClass =>
        val caseClassName = scalaType.typeSymbol.name.toString
        val typeArgs = scalaType.asInstanceOf[scala.reflect.runtime.universe.TypeRef].args
        val typeArgRefs = typeArgs.map(getTypeRef(_, typeParams))
        CaseClassRef(caseClassName, scalaType, typeArgRefs)
      case _ =>
        UnknownTypeRef(typeName, scalaType)
    }
  }

  private def getReferencedCaseClasses(typeRef: TypeRef): List[Type] = typeRef match {
    case CaseClassRef(name, t, typeArgs) => List(t) ++ typeArgs.flatMap(getReferencedCaseClasses)
    case SeqRef(innerType) => getReferencedCaseClasses(innerType)
    case OptionRef(innerType) => getReferencedCaseClasses(innerType)
    case _ => List.empty
  }


}
