package org.scalats.core

import java.io.PrintStream

import scala.reflect.api.Universe
import scala.reflect.runtime

/**
 * Created by Milosz on 11.06.2016.
 */
object TypeScriptGenerator {

  /**
   * Generates TypeScript from specified runtime classes.
   */
  def generateFromClassNames(
    config: Configuration,
    classNames: List[String],
    logger: Logger,
    out: String => PrintStream = _ => Console.out,
    classLoader: ClassLoader = getClass.getClassLoader): Unit = {
    import runtime.universe

    implicit def cl: ClassLoader = classLoader

    val mirror = universe.runtimeMirror(cl)
    val types = classNames.map { className =>
      mirror.staticClass(className).toType
    }

    generate(universe)(config, types, logger, out)
  }

  def generate[U <: Universe](universe: U)(
    config: Configuration,
    types: List[universe.Type],
    logger: Logger,
    out: String => PrintStream)(
    implicit
    cu: CompileUniverse[universe.type]): Unit = {
    val scalaParser = new ScalaParser[universe.type](universe, logger)
    val transpiler = new Transpiler(config)

    val scalaTypes = scalaParser.parseTypes(types)
    val typeScriptInterfaces = transpiler(scalaTypes)

    val emiter = new TypeScriptEmitter(config, out)

    emiter.emit(typeScriptInterfaces)
  }
}
