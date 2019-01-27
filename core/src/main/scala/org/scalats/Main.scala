package org.scalats

import org.scalats.configuration.Config

import org.scalats.core.{ Logger, TypeScriptGenerator }

object Main {
  def main(args: Array[String]): Unit = {
    val logger = Logger(
      org.slf4j.LoggerFactory getLogger TypeScriptGenerator.getClass)

    TypeScriptGenerator.generateFromClassNames(args.toList, logger)(Config())
  }
}
