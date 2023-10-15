package io.github.scalats.core

import scala.reflect.api.Universe

sealed trait CompileUniverse[U <: Universe] {
  def defaultMirror(universe: U): universe.Mirror
}

object CompileUniverse {
  import scala.reflect.api.JavaUniverse
  import scala.tools.nsc.Global

  implicit def globalUniverse[G <: Global]: CompileUniverse[G] =
    new CompileUniverse[G] {
      def defaultMirror(g: G): g.Mirror = g.rootMirror
    }

  private[scalats] implicit def javaUniverse[J <: JavaUniverse](
      implicit
      classLoader: ClassLoader
    ): CompileUniverse[J] = new CompileUniverse[J] {
    def defaultMirror(j: J): j.Mirror = j.runtimeMirror(classLoader)
  }
}
