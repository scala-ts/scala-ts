package io.github.scalats.core

trait Logger {
  def debug(msg: => String): Unit
  def info(msg: => String): Unit
  def warning(msg: => String): Unit
}

object Logger extends LoggerCompat {
  def apply(logger: org.slf4j.Logger): Logger = new Logger {
    def debug(msg: => String): Unit = logger.debug(msg)
    def info(msg: => String): Unit = logger.info(msg)
    def warning(msg: => String): Unit = logger.warn(msg)
  }
}
