package io.github.scalats

object UtilCompat {

  @inline def mapValues[K, V, U](m: Map[K, V])(f: V => U): Map[K, U] =
    m.view.mapValues(f).toMap
}
