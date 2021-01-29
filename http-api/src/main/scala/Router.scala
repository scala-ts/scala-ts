package io.github.scalats.demo

import play.api.libs.json.JsObject

import akka.http.scaladsl.server.Route

final class Router(context: AppContext) {
  import akka.http.scaladsl.server.Directives._

  // See https://github.com/lomigmegard/akka-http-cors#quick-start
  import ch.megard.akka.http.cors.scaladsl.CorsDirectives._

  import de.heikoseeberger.akkahttpplayjson.PlayJsonSupport._

  //import context.executor // ExecutionContext

  val instance: Route =
    logRequest(s"${context.name}.router") {
      // See https://doc.akka.io/docs/akka-http/current/routing-dsl/overview.html

      cors() {
        pathPrefix("user") {
          (post & path("signup") & entity(as[JsObject])) {
            signupRoute
          }
        }
      }
    }

  // ---

  private val signupRoute: JsObject => Route = { payload: JsObject =>
    complete(payload)
  }
}
