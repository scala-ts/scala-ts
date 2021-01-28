package io.github.scalats.demo.user

import scala.concurrent.Future

//import akka.http.scaladsl.server.Directives._

import akka.http.scaladsl.server.Route
import akka.http.scaladsl.server.directives.MethodDirectives.get
import akka.http.scaladsl.server.directives.RouteDirectives.complete

trait UserRoutes {
  protected lazy val userRoutes: Route = {
    // See https://doc.akka.io/docs/akka-http/current/routing-dsl/overview.html
    get {
      val result: Future[String] = Future.successful("Foo")

      complete(result)
    }
  }
}
