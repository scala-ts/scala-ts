package io.github.scalats.core

import io.github.scalats.ast

import ast.{ Declaration, TypeRef }

/**
 * The implementations must be class with a no-arg constructor.
 *
 * See:
 * - [[TypeMapper.ArrayAsGeneric]]
 * - [[TypeMapper.ArrayAsBrackets]]
 * - [[TypeMapper.DateAsString]]
 * - [[TypeMapper.NumberAsString]]
 * - [[TypeMapper.NullableAsOption]]
 */
trait TypeMapper
    extends Function5[
      TypeMapper.Resolved,
      Settings,
      Declaration,
      Field,
      TypeRef,
      Option[String]
    ] { self =>

  /**
   * @param parent the parent/fallback mapper
   * @param ownerType the name of the type which declared the member
   * @param member the name of the member
   * @param tpe a reference to the TypeScript type transpiled from Scala
   * @return Some TypeScript type, or none
   */
  def apply(
      parent: TypeMapper.Resolved,
      settings: Settings,
      ownerType: Declaration,
      member: Field,
      tpe: TypeRef
    ): Option[String]

  def andThen(m: TypeMapper): TypeMapper = new TypeMapper {

    @inline def apply(
        parent: TypeMapper.Resolved,
        settings: Settings,
        ownerType: Declaration,
        member: Field,
        tpe: TypeRef
      ): Option[String] =
      self(parent, settings, ownerType, member, tpe).orElse(
        m(parent, settings, ownerType, member, tpe)
      )
  }

  override def toString: String = getClass.getName
}

object TypeMapper {
  import com.github.ghik.silencer.silent

  /** `(settings, ownerType, member, type) => TypeScript type` */
  type Resolved =
    Function4[Settings, Declaration, Field, TypeRef, String]

  object Defaults extends TypeMapper {

    @silent @inline def apply(
        parent: TypeMapper.Resolved,
        settings: Settings,
        ownerType: Declaration,
        member: Field,
        tpe: TypeRef
      ) = Option.empty[String]
  }

  /** Emits Array as `Array<T>` */
  final class ArrayAsGeneric extends TypeMapper {

    def apply(
        parent: TypeMapper.Resolved,
        settings: Settings,
        ownerType: Declaration,
        member: Field,
        tpe: TypeRef
      ): Option[String] = tpe match {
      case ast.ArrayRef(innerType, false) =>
        Some(s"Array<${parent(settings, ownerType, member, innerType)}>")

      case ast.ArrayRef(innerType, true) => {
        val elmTpe = parent(settings, ownerType, member, innerType)

        Some(s"[${elmTpe}, ...Array<$elmTpe>]")
      }

      case _ => None
    }
  }

  lazy val arrayAsGeneric = new ArrayAsGeneric()

  /** Emits Array as `T[]` */
  final class ArrayAsBrackets extends TypeMapper {

    def apply(
        parent: TypeMapper.Resolved,
        settings: Settings,
        ownerType: Declaration,
        member: Field,
        tpe: TypeRef
      ): Option[String] = tpe match {
      case ast.ArrayRef(innerType, false) =>
        Some(s"${parent(settings, ownerType, member, innerType)}[]")

      case ast.ArrayRef(innerType, true) => {
        val elmTpe = parent(settings, ownerType, member, innerType)

        Some(s"[${elmTpe}, ...${elmTpe}[]]")
      }

      case _ => None
    }
  }

  lazy val arrayAsBrackets = new ArrayAsBrackets()

  final class NumberAsString extends TypeMapper {

    def apply(
        parent: TypeMapper.Resolved,
        settings: Settings,
        ownerType: Declaration,
        member: Field,
        tpe: TypeRef
      ): Option[String] = tpe match {
      case _: ast.NumberRef =>
        Some("string")

      case _ =>
        None
    }
  }

  lazy val numberAsString = new NumberAsString()

  final class DateAsString extends TypeMapper {

    def apply(
        parent: TypeMapper.Resolved,
        settings: Settings,
        ownerType: Declaration,
        member: Field,
        tpe: TypeRef
      ): Option[String] = tpe match {
      case ast.DateRef | ast.DateTimeRef =>
        Some("string")

      case _ =>
        None
    }
  }

  lazy val dateAsString = new DateAsString()

  /**
   * Maps [[ast.NullableType]] to `Option<T>`
   * (e.g. [[https://github.com/AlexGalays/space-monad space-monad]]
   * or [[https://gcanti.github.io/fp-ts/modules/Option.ts fp-ts]])
   */
  final class NullableAsOption extends TypeMapper {

    def apply(
        parent: TypeMapper.Resolved,
        settings: Settings,
        ownerType: Declaration,
        member: Field,
        tpe: TypeRef
      ): Option[String] = tpe match {
      case ast.NullableType(innerType) =>
        Some(s"Option<${parent(settings, ownerType, member, innerType)}>")

      case _ =>
        None
    }
  }

  lazy val nullableAsOption = new NullableAsOption()

  @SuppressWarnings(Array("UnsafeTraversableMethods" /*tail*/ ))
  def chain(multi: Seq[TypeMapper]): Option[TypeMapper] = {
    @scala.annotation.tailrec
    def go(
        in: Seq[TypeMapper],
        out: TypeMapper
      ): TypeMapper = in.headOption match {
      case Some(next) => go(in.tail, out.andThen(next))
      case _          => out
    }

    multi.headOption.map { first => go(multi.tail, first) }
  }
}
