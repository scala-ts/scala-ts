package com.mpc.scalats.configuration

import java.io.PrintStream

/**
  * Created by Milosz on 09.12.2016.
  */
case class Config(
  interfacePrefix: String = "IElium",
  optionToNullable: Boolean = true,
  optionToUndefined: Boolean = false,
  outputStream: Option[PrintStream] = None,
  customNameMap: Map[String, String] = Map("Metric"->"string"),
  leafTypes : Set[String] = Set.empty
)