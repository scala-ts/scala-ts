package io.github.scalats.core

import io.github.scalats.{ scala => ScalaModel }

import Internals.ListSet

private[scalats] trait ScalaParserCompat {
  import scala.language.higherKinds

  case class Result[M[_], Tpe](
      examined: ListSet[Tpe],
      parsed: M[ScalaModel.TypeDef]) {

    def mapParsed[N[_]](
        f: M[ScalaModel.TypeDef] => N[ScalaModel.TypeDef]
      ): Result[N, Tpe] =
      copy(parsed = f(parsed))
  }

  type TypeFullId = String

  type StringMap[T] = Map[String, ListSet[T]]
}
