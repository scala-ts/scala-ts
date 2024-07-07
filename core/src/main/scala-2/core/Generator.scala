package io.github.scalats.core

import scala.collection.immutable.Set

import scala.reflect.api.Universe

import Internals.ListSet

/**
 * Created by Milosz on 11.06.2016.
 *
 * @define settingsParam the generator settings
 * @define loggerParam the generator logger
 * @define printerParam the printer to output the generated TypeScript
 * @define declMapperParam the function to mapper the transpiled declaration
 * @define typeMapperParam the function to mapper the transpiled types to TypeScript code (if the standard emitter is used)
 */
object Generator {

  /**
   * Generates the TypeScript for the specified Scala `types`.
   *
   * @param settings $settingsParam
   * @param types the Scala types to be generated as TypeScript
   * @param logger $loggerParam
   * @param importResolver the import resolver to be used
   * @param declMapper $declMapperParam
   * @param typeMapper $typeMapperParam
   * @param printer $printerParam
   * @param examined the already examined type
   * @param compiled the already processed compilation units
   * @return the Scala types for which TypeScript has been emitted
   * (including the input `types` and the transitively required types).
   */
  @SuppressWarnings(Array("MaxParameters"))
  def generate[U <: Universe](
      universe: U
    )(settings: Settings,
      types: List[(universe.Type, universe.Tree)],
      symtab: Map[String, ListSet[(universe.Type, universe.Tree)]],
      logger: Logger,
      importResolver: ImportResolver,
      declMapper: DeclarationMapper,
      typeMapper: TypeMapper,
      printer: Printer,
      examined: ListSet[ScalaParser.TypeFullId],
      compiled: Set[String],
      acceptsType: universe.Symbol => Boolean
    )(implicit
      cu: CompileUniverse[universe.type]
    ): ListSet[ScalaParser.TypeFullId] = {
    val scalaParser = new ScalaParser[universe.type](universe, compiled, logger)
    val transpiler = new Transpiler(settings, logger)
    val parseResult =
      scalaParser.parseTypes(types, symtab, examined, acceptsType)

    import parseResult.{ parsed => scalaTypes }

    val typeScriptTypes = transpiler(scalaTypes)
    val emiter = new Emitter(
      settings,
      printer,
      importResolver,
      declMapper,
      typeMapper
    )

    emiter.emit(typeScriptTypes)

    parseResult.examined
  }
}
