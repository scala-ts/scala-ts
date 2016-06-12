package com.mpc.scalats

import com.mpc.scalats.core.TypeScriptGenerator

object Main {

  def main(args: Array[String]) {
    TypeScriptGenerator.generateFromClassNames(args.toList, Console.out)
  }

}
