package com.mpc.scalats.configuration

import java.io.PrintStream

/**
  * Created by Milosz on 09.12.2016.
  */
case class Config(
    emitInterfaces: Boolean = true,
    emitClasses: Boolean = false,
    optionToNullable: Boolean = true,
    optionToUndefined: Boolean = false,
    outputStream: Option[PrintStream] = None,
    prependIPrefix: Boolean = true
)
