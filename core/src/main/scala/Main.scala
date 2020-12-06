package org.scalats

import org.scalats.core.{ Configuration, Logger, TypeScriptGenerator }

object Main {
  def main(args: Array[String]): Unit = {
    val logger = Logger(
      org.slf4j.LoggerFactory getLogger TypeScriptGenerator.getClass)

    TypeScriptGenerator.generateFromClassNames(
      Configuration(), args.toList, logger)
  }
}
