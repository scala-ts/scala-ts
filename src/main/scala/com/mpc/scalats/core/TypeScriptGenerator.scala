package com.mpc.scalats.core

import java.io.PrintStream

import com.mpc.scalats.configuration.Config

import scala.reflect.runtime.universe._

/**
  * Created by Milosz on 11.06.2016.
  */
object TypeScriptGenerator {

  def generateFromClassNames(
                              classNames: List[String],
                              out: PrintStream,
                              classLoader: ClassLoader = getClass.getClassLoader
                            )
                            (implicit config: Config) = {
    implicit val mirror = runtimeMirror(classLoader)
    val types = classNames map { className =>
      mirror.staticClass(className).toType
    }
    generate(types, out)
  }

  def generate(caseClasses: List[Type], out: PrintStream)(implicit config: Config) = {
    val scalaCaseClasses = ScalaParser.parseCaseClasses(caseClasses)
    val typeScriptInterfaces = Compiler.compile(scalaCaseClasses)
    TypeScriptEmitter.emit(typeScriptInterfaces, Console.out)
  }

}
