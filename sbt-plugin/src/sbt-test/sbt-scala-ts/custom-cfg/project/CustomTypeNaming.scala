package scalats

import io.github.scalats.ast.{ ThisTypeRef, TypeRef }
import io.github.scalats.core.Settings

class CustomTypeNaming extends io.github.scalats.core.TypeNaming {

  def apply(settings: Settings, tpe: TypeRef) = {
    if (tpe.name == "this") {
      "this"
    } else {
      s"TS${tpe.name}"
    }
  }
}
