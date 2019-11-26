package com.mpc.scalats.core

/**
  * Created by Milosz on 09.06.2016.
  */

import com.mpc.scalats.configuration.Config
import org.slf4j.LoggerFactory

import scala.collection.mutable
import scala.collection.mutable.ArrayBuffer
import scala.reflect.runtime.universe._

case class TypeToParse(t: Type, parent: Option[Type]) {

  def typeSymbol = t.typeSymbol

  def members = t.members

  def typeConstructor = t.typeConstructor
}

/*
Parses a set of types to construct typings.

It has 3 important components :

- analyseType will recursively find all types in a given type scope.
For example if you have case classes where members are themselves case classes, it will descend this tree
- getInvolvedTypes analyse a specific case class to find type arguments involved in it, through type params and generic members

After those two have run, we have a big lot of TypeToParse, which we can then call parse on, to construct type summaries ready to be sent
to the typescript emitter.

 */
object ScalaParser {

  private val logger = LoggerFactory.getLogger(getClass)

  private[core] var alreadyExamined: mutable.Set[String] = mutable.Set.empty

  import ScalaModel._

  def parseTypes(userInputTypes: List[Type])(implicit config: Config): List[CaseClass] = {
    alreadyExamined ++= config.leafTypes
    val initialTypes = userInputTypes.map(t => TypeToParse(t, None))
    val allInvolvedTypes = initialTypes.flatMap(analyseType).distinct
    allInvolvedTypes.map(parse)
  }

  private def isSealedTrait(scalaType: TypeToParse) = {
    scalaType.typeSymbol.asClass.isSealed &&
      scalaType.typeSymbol.asClass.isTrait
  }

  private def analyseType(analysedType: TypeToParse)(implicit config: Config): List[TypeToParse] = {
    if (!alreadyExamined.exists(x => x == analysedType.typeSymbol.fullName)) {
      alreadyExamined.add(analysedType.t.typeSymbol.fullName)
      if (isSealedTrait(analysedType)) {
        analysedType :: knownSubclasses(analysedType).toList.map(
          t => TypeToParse(t, Some(analysedType.t))
        ).flatMap(analyseType)

      } else if (isCaseClass(analysedType)) {
        analysedType :: getInvolvedTypes(analysedType).flatMap(analyseType)
      } else {
        Nil
      }
    } else {
      Nil
    }
  }

  private def knownSubclasses(tpe: TypeToParse): Set[Type] = {
    // Workaround for SI-7046: https://issues.scala-lang.org/browse/SI-7046

    def allSubclasses(parentClass: ClassSymbol): Set[Type] = {
      val subs = parentClass.knownDirectSubclasses
      subs.flatMap { sym =>
        sym match {
          case cls: ClassSymbol if parentClass != cls && cls.selfType.baseClasses.contains(parentClass) => {
            Set(cls.typeSignature) ++ allSubclasses(cls)
          }

          case o: ModuleSymbol if o.companion == NoSymbol && // not a companion object
            o.typeSignature.baseClasses.contains(parentClass) => {
            Set(o.typeSignature)
          }

          case _ => Set.empty[Type]
        }
      }
    }

    val tpeSym = tpe.typeSymbol.asClass
    if (tpeSym.isSealed && tpeSym.isAbstract && tpeSym.isTrait) {
      allSubclasses(tpeSym)
    } else Set.empty
  }

  private def getInvolvedTypes(typeToParse: TypeToParse)(implicit config: Config): List[TypeToParse] = {

    /*
    Finds all "involved" types. That is :

    - types that are type arguments of the class
    - types that are types of case class members
    - types that are types arguments of such two things (recurisvely).

    So for example if you have a member or type arg that is an Option[Class1[Class2]] it will detect Option, Class1, and Class2 through the recursion.
     */
    def getInvolvedTypes(locallyExamined: Set[Type])(scalaType: Type)(implicit config: Config): List[Type] = {
      if (!locallyExamined.contains(scalaType) && !scalaType.typeSymbol.isParameter && !config.leafTypes.contains(scalaType.typeSymbol.fullName)) {
        val relevantMemberSymbols = scalaType.members.collect {
          case m: MethodSymbol if m.isCaseAccessor => m
        }
        val memberTypes = relevantMemberSymbols.map(_.typeSignature match {
          case NullaryMethodType(resultType) => resultType
          case t => t
        }).flatMap(getInvolvedTypes(locallyExamined + scalaType))
        val typeArgs = scalaType match {
          case t: scala.reflect.runtime.universe.TypeRef => t.args.flatMap(getInvolvedTypes(locallyExamined + scalaType))
          case _ => List.empty
        }
        (scalaType.typeConstructor :: typeArgs ::: memberTypes.toList).filter(!_.typeSymbol.isParameter).distinct
      } else {
        List.empty
      }
    }

    getInvolvedTypes(Set.empty)(typeToParse.t).map(t => TypeToParse(t, None))

  }

  private def parse(typeToParse: TypeToParse): CaseClass = {
    if (isSealedTrait(typeToParse)) {
      parseSealedTrait(typeToParse)
    } else if (isCaseClass(typeToParse)) {
      parseCaseClass(typeToParse)
    } else {
      throw new Exception(s"Unable to parse type ${typeToParse.t.typeSymbol.name} : it's neither a case class or a sealed trait")
    }
  }

  private def parseSealedTrait(sealedTraitType: TypeToParse): CaseClass = {
    val typeParams = sealedTraitType.typeConstructor.normalize match {
      case polyType: PolyTypeApi => polyType.typeParams.map(_.name.decoded)
      case _ => List.empty[String]
    }
    CaseClass(
      sealedTraitType.typeSymbol.name.toString,
      Nil,
      typeParams
    )
  }

  private def parseCaseClass(caseClassType: TypeToParse): CaseClass = {

    alreadyExamined += caseClassType.typeSymbol.name.toString

    val relevantMemberSymbols = caseClassType.members.collect {
      case m: MethodSymbol if m.isCaseAccessor => m
    }
    val typeParams = caseClassType.typeConstructor.normalize match {
      case polyType: PolyTypeApi => polyType.typeParams.map(_.name.decoded)
      case _ => List.empty[String]
    }
    val members = relevantMemberSymbols map { member =>
      val memberName = member.name.toString
      CaseClassMember(memberName, getTypeRef(member.returnType, typeParams.toSet))
    }

    CaseClass(
      caseClassType.typeSymbol.name.toString,
      members.toList,
      typeParams,
      caseClassType.parent.map(_.typeSymbol.name.toString)
    )
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
      case "Float" =>
        FloatRef
      case "BigDecimal" =>
        BigDecimalRef
      case "Boolean" =>
        BooleanRef
      case "String" | "UUID" =>
        StringRef
      case "List" | "Seq" | "Set" =>
        val innerType = scalaType.asInstanceOf[scala.reflect.runtime.universe.TypeRef].args.head
        SeqRef(getTypeRef(innerType, typeParams))
      case "Option" =>
        val innerType = scalaType.asInstanceOf[scala.reflect.runtime.universe.TypeRef].args.head
        OptionRef(getTypeRef(innerType, typeParams))
      case "LocalDate" =>
        DateRef
      case "Instant" | "Timestamp" =>
        DateTimeRef
      case typeParam if typeParams.contains(typeParam) =>
        TypeParamRef(typeParam)
      case _ if isCaseClass(scalaType) =>
        val caseClassName = scalaType.typeSymbol.name.toString
        val typeArgs = scalaType.asInstanceOf[scala.reflect.runtime.universe.TypeRef].args
        val typeArgRefs = typeArgs.map(getTypeRef(_, typeParams))
        CaseClassRef(caseClassName, typeArgRefs)
      case _ =>
        UnknownTypeRef(typeName)
    }
  }

  private def isCaseClass(scalaType: TypeToParse): Boolean = isCaseClass(scalaType.t)

  private def isCaseClass(scalaType: Type): Boolean = scalaType.typeSymbol.isClass && scalaType.typeSymbol.asClass.isCaseClass

}
