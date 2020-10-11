package org.scalats.core

import scala.reflect.api.Universe

sealed trait CompileUniverse[U <: Universe] {
  def defaultMirror(universe: U): universe.Mirror
}

object CompileUniverse {
  import scala.reflect.api.JavaUniverse
  import scala.tools.nsc.Global

  implicit def globalUniverse[G <: Global] = new CompileUniverse[G] {
    def defaultMirror(g: G) = g.rootMirror
  }

  implicit def javaUniverse[J <: JavaUniverse](
    implicit
    classLoader: ClassLoader) = new CompileUniverse[J] {
    def defaultMirror(j: J): j.Mirror = j.runtimeMirror(classLoader)
  }
}

