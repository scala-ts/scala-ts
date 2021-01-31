package io.github.scalats.demo

import io.github.scalats.demo.model.Account

import akka.http.scaladsl.model.{ headers, StatusCodes }
import akka.http.scaladsl.server.{
  RejectionHandler,
  Route,
  UnsupportedRequestContentTypeRejection,
  ValidationRejection
}
import play.api.libs.json._

final class Router(context: AppContext) {
  import akka.http.scaladsl.server.Directives._

  // See https://github.com/lomigmegard/akka-http-cors#quick-start
  import ch.megard.akka.http.cors.scaladsl.CorsDirectives._

  import de.heikoseeberger.akkahttpplayjson.PlayJsonSupport._

  //import context.executor // ExecutionContext

  import Codecs._

  val instance: Route = {
    implicit val rejHandler: RejectionHandler = rejectionHandler

    Route.seal(logRequest(s"${context.name}.router") {
      // See https://doc.akka.io/docs/akka-http/current/routing-dsl/overview.html

      cors() {
        concat(
          pathPrefix("user") {
            (post & path("signup") & entity(as[Account])) {
              signupRoute
            }

            // TODO: POST signin(Credentials)
            // TODO: GET profile
          },
          get {
            concat(
              pathPrefix("images") {
                staticResources("images")
              },
              pathPrefix("immutable") {
                staticResources("immutable")
              },
              getFromResource("webroot/index.html")
            )
          }
        )
      }
    })
  }

  // ---

  private def staticResources(prefix: String) =
    extractUnmatchedPath { path =>
      val res = path.toString.stripPrefix("/")

      getFromResource(s"webroot/${prefix}/${res}")
    }

  private val signupRoute: Account => Route = { account: Account =>
    val existing = context.cache.getIfPresent(account.userName)

    if (existing != null) {
      complete(
        StatusCodes.Forbidden,
        Json.obj("error" -> "forbidden", "details" -> "User already created")
      )

    } else {
      context.cache.put(account.userName, account)

      respondWithHeader(
        headers.`Cache-Control`(
          headers.CacheDirectives.`max-age`(context.cacheDuration.getSeconds)
        )
      ) {
        complete(StatusCodes.Created, account.userName)
      }
    }
  }

  // ---

  private lazy val rejectionHandler = RejectionHandler
    .newBuilder()
    .handle {
      case UnsupportedRequestContentTypeRejection(supported) =>
        complete(
          StatusCodes.BadRequest,
          Json.obj(
            "error" -> "requestContentType",
            "details" -> Json.obj("supported" -> supported)
          )
        )

      case ValidationRejection(message, _) =>
        complete(
          StatusCodes.InternalServerError,
          Json.obj("error" -> "validation", "details" -> Json.parse(message))
        )

      case rej =>
        sys.error(s"${rej.getClass}: $rej")
    }
    .result()
}
