package org.scalats.core

import org.scalats.configuration.Config

import scala.reflect.runtime.universe._

object TypeScriptGenerator {

  def generateFromClassNames(
    classNames: List[String],
    logger: Logger,
    classLoader: ClassLoader = getClass.getClassLoader
  )(implicit config: Config) = {
    val mirror = runtimeMirror(classLoader)
    val types = classNames.map { className =>
      mirror.staticClass(className).toType
    }

    generate(types, logger, mirror)
  }

  def generate(caseClasses: List[Type], logger: Logger, mirror: Mirror)(implicit config: Config): Unit = {
    val outputStream = config.outputStream.getOrElse(Console.out)
    val scalaParser = new ScalaParser(logger, mirror)
    val scalaTypes = scalaParser.parseTypes(caseClasses)
    val typeScriptInterfaces = Compiler.compile(scalaTypes)

    val emiter = new TypeScriptEmitter(config)

    emiter.emit(typeScriptInterfaces, outputStream)
  }
}
