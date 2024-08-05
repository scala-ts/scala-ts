package io.github.scalats.core

import scala.collection.immutable.ListSet

import dotty.tools.dotc.core.Contexts.Context
import dotty.tools.dotc.core.Symbols.Symbol
import dotty.tools.dotc.core.Types.Type

import dotty.tools.dotc.ast.tpd

object Generator:
  import tpd._

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
  def generate(
      settings: Settings,
      types: List[(Type, Tree)],
      symtab: Map[String, ListSet[(Type, Tree)]],
      logger: Logger,
      importResolver: ImportResolver,
      declMapper: DeclarationMapper,
      typeMapper: TypeMapper,
      printer: Printer,
      examined: ListSet[ScalaParser.TypeFullId],
      compiled: Set[String],
      acceptsType: Symbol => Boolean
    )(using
      Context
    ): ListSet[ScalaParser.TypeFullId] = {
    val scalaParser = new ScalaParser(compiled, logger)
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

end Generator
