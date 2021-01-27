package scalats

import io.github.scalats.typescript.TypeRef
import io.github.scalats.core.Settings

class CustomTypeNaming extends io.github.scalats.core.TypeScriptTypeNaming {
  def apply(settings: Settings, tpe: TypeRef) = s"TS${tpe.name}"
}
