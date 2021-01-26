package io.github.scalats.core

import scala.collection.immutable.ListSet

import scala.reflect.api.Universe
//import scala.reflect.runtime

/**
 * Created by Milosz on 11.06.2016.
 *
 * @define settingsParam the generator settings
 * @define loggerParam the generator logger
 * @define printerParam the printer to output the generated TypeScript
 * @define declMapperParam the function to mapper the transpiled declaration
 * @define typeMapperParam the function to mapper the transpiled types to TypeScript code (if the standard emitter is used)
 */
object TypeScriptGenerator {

  /* TODO: Remove of refactor with runtime ToolBox
   * Generates TypeScript from specified runtime classes.
   *
   * @param settings $settingsParam
   * @param classNames the names of Scala classes to be generated as TypeScript
   * @param logger $loggerParam
   * @param printer $printerParam
   * @param declMapper $declMapperParam
   * @param typeMapper $typeMapperParam
  def generateFromClassNames(
    settings: Settings,
    classNames: List[String],
    logger: Logger,
    printer: TypeScriptPrinter = TypeScriptPrinter.StandardOutput,
    declMapper: TypeScriptDeclarationMapper = TypeScriptDeclarationMapper.Defaults,
    typeMapper: TypeScriptTypeMapper = TypeScriptTypeMapper.Defaults,
    classLoader: ClassLoader = getClass.getClassLoader //
  ): ListSet[ScalaParser.TypeFullId] = {
    import runtime.universe

    implicit def cl: ClassLoader = classLoader

    val mirror = universe.runtimeMirror(cl)
    val types = classNames.map { className =>
      mirror.staticClass(className).toType
    }

    generate(universe)(
      settings, types, logger, declMapper,
      typeMapper, printer, ListSet.empty)
  }
   */

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
   * @return the Scala types for which TypeScript has been emitted
   * (including the input `types` and the transitively required types).
   */
  @SuppressWarnings(Array("MaxParameters"))
  def generate[U <: Universe](universe: U)(
    settings: Settings,
    types: List[(universe.Type, universe.Tree)],
    symtab: Map[String, (universe.Type, universe.Tree)],
    logger: Logger,
    importResolver: TypeScriptImportResolver,
    declMapper: TypeScriptDeclarationMapper,
    typeMapper: TypeScriptTypeMapper,
    printer: TypeScriptPrinter,
    examined: ListSet[ScalaParser.TypeFullId])(
    implicit
    cu: CompileUniverse[universe.type]): ListSet[ScalaParser.TypeFullId] = {
    val scalaParser = new ScalaParser[universe.type](universe, logger)
    val transpiler = new Transpiler(settings)

    val parseResult = scalaParser.parseTypes(types, symtab, examined)
    import parseResult.{ parsed => scalaTypes }

    val typeScriptTypes = transpiler(scalaTypes)
    val emiter = new TypeScriptEmitter(
      settings, printer, importResolver, declMapper, typeMapper)

    emiter.emit(typeScriptTypes)

    parseResult.examined
  }
}
