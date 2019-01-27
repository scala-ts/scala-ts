package org.scalats.core

trait Logger {
  def warning(msg: => String): Unit
}

object Logger {
  def apply(logger: org.slf4j.Logger): Logger = new Logger {
    def warning(msg: => String): Unit = logger.warn(msg)
  }
}
