package io.github.scalats.core

trait Logger {
  def info(msg: => String): Unit
  def warning(msg: => String): Unit
}

object Logger extends LoggerCompat {
  def apply(logger: org.slf4j.Logger): Logger = new Logger {
    def info(msg: => String): Unit = logger.info(msg)
    def warning(msg: => String): Unit = logger.warn(msg)
  }
}
