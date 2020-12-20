package io.github.scalats.core

import scala.collection.immutable.ListSet

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
    out: TypeScriptPrinter = TypeScriptPrinter.StandardOutput,
    typeMapper: TypeScriptTypeMapper = TypeScriptTypeMapper.Defaults,
    classLoader: ClassLoader = getClass.getClassLoader //
  ): ListSet[ScalaParser.TypeFullId] = {
    import runtime.universe

    implicit def cl: ClassLoader = classLoader

    val mirror = universe.runtimeMirror(cl)
    val types = classNames.map { className =>
      mirror.staticClass(className).toType
    }

    generate(universe)(config, types, logger, out, typeMapper, ListSet.empty)
  }

  /**
   * Generates the TypeScript for the specified Scala `types`.
   *
   * @param examined the already examined type
   * @return the Scala types for which TypeScript has been emitted
   * (including the input `types` and the transitively required types).
   */
  def generate[U <: Universe](universe: U)(
    config: Configuration,
    types: List[universe.Type],
    logger: Logger,
    out: TypeScriptPrinter,
    typeMapper: TypeScriptTypeMapper,
    examined: ListSet[ScalaParser.TypeFullId])(
    implicit
    cu: CompileUniverse[universe.type]): ListSet[ScalaParser.TypeFullId] = {
    val scalaParser = new ScalaParser[universe.type](universe, logger)
    val transpiler = new Transpiler(config)

    val parseResult = scalaParser.parseTypes(types, examined)
    import parseResult.{ parsed => scalaTypes }

    val typeScriptTypes = transpiler(scalaTypes)
    val emiter = new TypeScriptEmitter(config, out, typeMapper)

    emiter.emit(typeScriptTypes)

    parseResult.examined
  }
}
