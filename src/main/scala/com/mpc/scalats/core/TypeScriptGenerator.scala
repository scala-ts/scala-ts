package com.mpc.scalats.core

import java.io.{File, PrintStream}

import com.mpc.scalats.configuration.Config

import scala.reflect.runtime.universe._

/**
  * Created by Milosz on 11.06.2016.
  */
object TypeScriptGenerator {

  def generateFromClassNames(
                              classNames: List[String],
                              classLoader: ClassLoader = getClass.getClassLoader
                            )
                            (implicit config: Config) = {
    implicit val mirror = runtimeMirror(classLoader)
    val types = classNames map { className =>
      mirror.staticClass(className).toType
    }
    generate(types)(config)
  }

  def generate(caseClasses: List[Type])(implicit config: Config) = {
    val outputStream = config.outputStream.getOrElse(Console.out)
    val scalaCaseClasses = ScalaParser.parseCaseClasses(caseClasses)
    val typeScriptInterfaces = Compiler.compile(scalaCaseClasses)
    TypeScriptEmitter.emit(typeScriptInterfaces, outputStream)
  }

}
