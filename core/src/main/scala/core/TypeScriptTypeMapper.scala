package io.github.scalats.core

import io.github.scalats.typescript

import typescript.TypeRef

/**
 * The implementations must be class with a no-arg constructor.
 *
 * See:
 * - [[TypeScriptTypeMapper.ArrayAsGeneric]]
 * - [[TypeScriptTypeMapper.ArrayAsBrackets]]
 * - [[TypeScriptTypeMapper.DateAsString]]
 * - [[TypeScriptTypeMapper.NumberAsString]]
 * - [[TypeScriptTypeMapper.NullableAsOption]]
 */
trait TypeScriptTypeMapper extends Function4[TypeScriptTypeMapper.Resolved, String, TypeScriptField, TypeRef, Option[String]] { self =>

  /**
   * @param parent the parent/fallback mapper
   * @param ownerType the name of the type which declared the member
   * @param member the name of the member
   * @param tpe a reference to the TypeScript type transpiled from Scala
   * @return Some TypeScript type, or none
   */
  def apply(
    parent: TypeScriptTypeMapper.Resolved,
    ownerType: String,
    member: TypeScriptField,
    tpe: TypeRef): Option[String]

  def andThen(m: TypeScriptTypeMapper): TypeScriptTypeMapper =
    new TypeScriptTypeMapper {
      @inline def apply(
        parent: TypeScriptTypeMapper.Resolved,
        ownerType: String,
        member: TypeScriptField,
        tpe: TypeRef): Option[String] =
        self(parent, ownerType, member, tpe).
          orElse(m(parent, ownerType, member, tpe))
    }
}

object TypeScriptTypeMapper {
  import com.github.ghik.silencer.silent

  /** `(ownerType, member, type) => TypeScript type` */
  type Resolved = Function3[String, TypeScriptField, TypeRef, String]

  object Defaults extends TypeScriptTypeMapper {
    @silent @inline def apply(
      parent: TypeScriptTypeMapper.Resolved,
      ownerType: String,
      member: TypeScriptField,
      tpe: TypeRef) = Option.empty[String]
  }

  /** Emit Array as `Array<T>` */
  final class ArrayAsGeneric extends TypeScriptTypeMapper {
    def apply(
      parent: TypeScriptTypeMapper.Resolved,
      ownerType: String,
      member: TypeScriptField,
      tpe: TypeRef): Option[String] = tpe match {
      case typescript.ArrayRef(innerType) =>
        Some(s"Array<${parent(ownerType, member, innerType)}>")

      case _ => None
    }
  }

  lazy val arrayAsGeneric = new ArrayAsGeneric()

  /** Emit Array as `T[]` */
  final class ArrayAsBrackets extends TypeScriptTypeMapper {
    def apply(
      parent: TypeScriptTypeMapper.Resolved,
      ownerType: String,
      member: TypeScriptField,
      tpe: TypeRef): Option[String] = tpe match {
      case typescript.ArrayRef(innerType) =>
        Some(s"${parent(ownerType, member, innerType)}[]")

      case _ => None
    }
  }

  lazy val arrayAsBrackets = new ArrayAsBrackets()

  final class NumberAsString extends TypeScriptTypeMapper {
    def apply(
      parent: TypeScriptTypeMapper.Resolved,
      ownerType: String,
      member: TypeScriptField,
      tpe: TypeRef): Option[String] = tpe match {
      case typescript.NumberRef =>
        Some("string")

      case _ =>
        None
    }
  }

  lazy val numberAsString = new NumberAsString()

  final class DateAsString extends TypeScriptTypeMapper {
    def apply(
      parent: TypeScriptTypeMapper.Resolved,
      ownerType: String,
      member: TypeScriptField,
      tpe: TypeRef): Option[String] = tpe match {
      case typescript.DateRef | typescript.DateTimeRef =>
        Some("string")

      case _ =>
        None
    }
  }

  lazy val dateAsString = new DateAsString()

  /**
   * Maps [[typescript.NullableType]] to `Option<T>`
   * (e.g. [[https://github.com/AlexGalays/space-monad space-monad]]
   * or [[https://gcanti.github.io/fp-ts/modules/Option.ts fp-ts]])
   */
  final class NullableAsOption extends TypeScriptTypeMapper {
    def apply(
      parent: TypeScriptTypeMapper.Resolved,
      ownerType: String,
      member: TypeScriptField,
      tpe: TypeRef): Option[String] = tpe match {
      case typescript.NullableType(innerType) =>
        Some(s"Option<${parent(ownerType, member, innerType)}>")

      case _ =>
        None
    }
  }

  lazy val nullableAsOption = new NullableAsOption()

  def chain(multi: Seq[TypeScriptTypeMapper]): Option[TypeScriptTypeMapper] = {
    @scala.annotation.tailrec def go(
      in: Seq[TypeScriptTypeMapper],
      out: TypeScriptTypeMapper): TypeScriptTypeMapper = in.headOption match {
      case Some(next) => go(in.tail, out.andThen(next))
      case _ => out
    }

    multi.headOption.map { first =>
      go(multi.tail, first)
    }
  }
}
