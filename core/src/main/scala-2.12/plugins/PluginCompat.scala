package io.github.scalats.plugins

import scala.tools.nsc.plugins.Plugin

private[plugins] trait PluginCompat { _: Plugin =>
  protected def warning(msg: => String) = global.warning(msg)
}
