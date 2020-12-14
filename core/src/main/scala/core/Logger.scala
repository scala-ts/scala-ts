package io.github.scalats.core

trait Logger {
  def warning(msg: => String): Unit
}

object Logger extends LoggerCompat {
  def apply(logger: org.slf4j.Logger): Logger = new Logger {
    def warning(msg: => String): Unit = logger.warn(msg)
  }
}
