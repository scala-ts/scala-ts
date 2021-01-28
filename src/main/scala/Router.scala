package io.github.scalats.demo

import akka.http.scaladsl.server.Route

final class Router(context: AppContext) {
  import akka.http.scaladsl.server.Directives._

  // See https://github.com/lomigmegard/akka-http-cors#quick-start
  import ch.megard.akka.http.cors.scaladsl.CorsDirectives._

  //import de.heikoseeberger.akkahttpplayjson.PlayJsonSupport._

  //import context.executor // ExecutionContext

  val instance: Route =
    logRequest(s"${context.name}.router") {
      // See https://doc.akka.io/docs/akka-http/current/routing-dsl/overview.html

      cors() {
        ???
      }
    }
}
