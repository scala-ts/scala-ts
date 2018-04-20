package com.mpc.scalats

import com.mpc.scalats.configuration.Config

import com.mpc.scalats.core.{ Logger, TypeScriptGenerator }

object Main {
  def main(args: Array[String]): Unit = {
    val logger = Logger(
      org.slf4j.LoggerFactory getLogger TypeScriptGenerator.getClass)

    TypeScriptGenerator.generateFromClassNames(args.toList, logger)(Config())
  }
}
