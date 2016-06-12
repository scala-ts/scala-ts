package com.mpc.scalats.core

import java.io.PrintStream

import scala.reflect.runtime.universe._

/**
  * Created by Milosz on 11.06.2016.
  */
object TypeScriptGenerator {

  def generateFromClassNames(classNames: List[String],
                             out: PrintStream,
                             classLoader: ClassLoader = getClass.getClassLoader) = {
    implicit val mirror = runtimeMirror(classLoader)
    val types = classNames map { className =>
      mirror.staticClass(className).toType
    }
    generate(types, out)
  }

  def generate(caseClasses: List[Type], out: PrintStream) = {
//    logger.info("Running parser for the following types:")
    caseClasses foreach { caseClass =>
//      logger.info(caseClass.typeSymbol.name.toString)
    }
    val scalaCaseClasses = ScalaParser.parseCaseClasses(caseClasses).values.toList
//    logger.info("Compiling...")
    val typeScriptInterfaces = Compiler.compile(scalaCaseClasses)
//    logger.info("Writing TypeScript code...")
    Emitter.emit(typeScriptInterfaces, Console.out)
  }

}
