package io.github.scalats.demo

import akka.http.scaladsl.server.Route

import user.UserRoutes

final class Router(context: AppContext) extends UserRoutes {

  import akka.http.scaladsl.server.Directives._

  val instance: Route =
    logRequest(s"${context.name}.router") {
      // See https://doc.akka.io/docs/akka-http/current/routing-dsl/overview.html
      {
        pathPrefix("user")(userRoutes)
      } /* ~ {
      pathPrefix("other")(otherRoutes)
    }
       */
    }
}
