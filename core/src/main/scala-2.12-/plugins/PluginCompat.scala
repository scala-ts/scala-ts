package org.scalats.plugins

import scala.tools.nsc.plugins.Plugin

private[plugins] trait PluginCompat { _: Plugin =>
  protected def init(
    options: List[String],
    error: String => Unit): Boolean

  @inline final override def processOptions(
    options: List[String],
    error: String => Unit): Unit = {
    init(options, error)
    ()
  }

  protected def warning(msg: => String) = global.warning(msg)
}

