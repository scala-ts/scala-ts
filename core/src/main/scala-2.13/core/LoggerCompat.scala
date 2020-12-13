package org.scalats.core

private[core] trait LoggerCompat { _: Logger.type =>
  def apply(global: scala.tools.nsc.Global): Logger = new Logger {
    def warning(msg: => String): Unit = global.devWarning(msg)
  }
}
