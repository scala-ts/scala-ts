package io.github.scalats.plugins

import io.github.scalats.core.Logger

private[scalats] final class DeferredLogger extends Logger {
  private val initBuffer = Seq.newBuilder[(String, String)]

  def debug(msg: => String): Unit = _debug(msg)
  def info(msg: => String): Unit = _info(msg)
  def warning(msg: => String): Unit = _warn(msg)

  private def logToInitBuffer(
      level: String
    ): String => Unit = { (msg: String) =>
    this.synchronized {
      initBuffer += level -> msg
    }
  }

  private var _debug: String => Unit = logToInitBuffer("debug")
  private var _info: String => Unit = logToInitBuffer("info")
  private var _warn: String => Unit = logToInitBuffer("warning")

  private[scalats] def apply(inited: Logger): Unit = {
    val deferred: Seq[(String, String)] = this.synchronized {
      val entries = initBuffer.result()

      initBuffer.clear()

      _debug = { (msg: String) => inited.debug(msg) }
      _info = { (msg: String) => inited.info(msg) }
      _warn = { (msg: String) => inited.warning(msg) }

      entries
    }

    deferred.foreach {
      case ("debug", msg) =>
        inited.debug(msg)

      case ("warning", msg) =>
        inited.warning(msg)

      case (_ /*info*/, msg) =>
        inited.info(msg)
    }
  }
}
