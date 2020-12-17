package io.github.scalats.core

import io.github.scalats.core.TypeScriptModel.TypeRef

/**
 * The implementations must be class with a no-arg constructor.
 */
trait TypeScriptTypeMapper extends Function4[TypeScriptTypeMapper.Resolved, String, String, TypeRef, Option[String]] { self =>

  /**
   * @param parent the parent/fallback mapper
   * @param ownerType the name of the type which declared the member
   * @param memberName the name of the member
   * @param tpe a reference to the TypeScript type transpiled from Scala
   * @return Some TypeScript type, or none
   */
  def apply(
    parent: TypeScriptTypeMapper.Resolved,
    ownerType: String,
    memberName: String,
    tpe: TypeRef): Option[String]

  def andThen(m: TypeScriptTypeMapper): TypeScriptTypeMapper =
    new TypeScriptTypeMapper {
      @inline def apply(
        parent: TypeScriptTypeMapper.Resolved,
        ownerType: String,
        memberName: String,
        tpe: TypeRef): Option[String] =
        self(parent, ownerType, memberName, tpe).
          orElse(m(parent, ownerType, memberName, tpe))
    }
}

object TypeScriptTypeMapper {
  import com.github.ghik.silencer.silent

  type Resolved = Function3[String, String, TypeRef, String]

  object Defaults extends TypeScriptTypeMapper {
    @silent @inline def apply(
      parent: TypeScriptTypeMapper.Resolved,
      ownerType: String,
      memberName: String,
      tpe: TypeRef) = Option.empty[String]
  }

  final class NumberAsString extends TypeScriptTypeMapper {
    def apply(
      parent: TypeScriptTypeMapper.Resolved,
      ownerType: String,
      memberName: String,
      tpe: TypeRef): Option[String] = tpe match {
      case TypeScriptModel.NumberRef =>
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
      memberName: String,
      tpe: TypeRef): Option[String] = tpe match {
      case TypeScriptModel.DateRef | TypeScriptModel.DateTimeRef =>
        Some("string")

      case _ =>
        None
    }
  }

  lazy val dateAsString = new DateAsString()

  /**
   * Maps [[TypeScriptModel.NullableType]] to `Option<T>`
   * (e.g. [[https://github.com/AlexGalays/space-monad space-monad]]
   * or [[https://gcanti.github.io/fp-ts/modules/Option.ts fp-ts]])
   */
  final class NullableAsOption extends TypeScriptTypeMapper {
    def apply(
      parent: TypeScriptTypeMapper.Resolved,
      ownerType: String,
      memberName: String,
      tpe: TypeRef): Option[String] = tpe match {
      case TypeScriptModel.NullableType(innerType) =>
        Some(s"Option<${parent(ownerType, memberName, innerType)}>")

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
