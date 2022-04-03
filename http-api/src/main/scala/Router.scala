package io.github.scalats.demo

import akka.http.scaladsl.model.{ headers, StatusCodes }
import akka.http.scaladsl.server.{
  RejectionHandler,
  Route,
  UnsupportedRequestContentTypeRejection,
  ValidationRejection
}
import akka.http.scaladsl.server.directives.{ Credentials => Creds }

import io.github.scalats.demo.model.{ Account, Credentials, UserName }

import play.api.libs.json._

final class Router(context: AppContext) {
  import akka.http.scaladsl.server.Directives._

  // See https://github.com/lomigmegard/akka-http-cors#quick-start
  import ch.megard.akka.http.cors.scaladsl.CorsDirectives._

  import de.heikoseeberger.akkahttpplayjson.PlayJsonSupport._

  // import context.executor // ExecutionContext

  import Codecs._

  val instance: Route = {
    implicit val rejHandler: RejectionHandler = rejectionHandler

    Route.seal(logRequest(s"${context.name}.router") {
      // See https://doc.akka.io/docs/akka-http/current/routing-dsl/overview.html

      cors() {
        concat(
          pathPrefix("user") {
            concat(
              (post & path("signup") & entity(as[Account])) {
                signupRoute
              },
              authenticateBasic(
                realm = "scala-ts-demo",
                Authenticator.unapply
              ) { account =>
                (get & path("profile")) {
                  complete(Json toJson account)
                }
              }
            )
          },
          (post & path("signin") & entity(as[Credentials])) {
            signinRoute
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

  private object Authenticator {
    def unapply: Creds => Option[Account] = (_: Creds) match {
      case p @ Creds.Provided(name) =>
        findUser(new UserName(name)).filter { user =>
          p.verify(Digest.md5Hex(user.password, "UTF-8"))
        }

      case _ =>
        None
    }
  }

  private val signupRoute: Account => Route = { account =>
    if (findUser(account.userName).nonEmpty) {
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

  private val signinRoute: Credentials => Route = { credentials =>
    import credentials.{ userName, password }

    findUser(userName).filter(_.password == password) match {
      case Some(_) =>
        complete(Json toJson s"${userName}:${Digest.md5Hex(password, "UTF-8")}")

      case _ =>
        complete(
          StatusCodes.Forbidden,
          Json.obj("error" -> "forbidden", "details" -> "Unauthorized")
        )
    }
  }

  private def findUser(userName: UserName): Option[Account] =
    Option(context.cache getIfPresent userName)

  // To embed frontend as static resources
  private def staticResources(prefix: String) =
    extractUnmatchedPath { path =>
      val res = path.toString.stripPrefix("/")

      getFromResource(s"webroot/${prefix}/${res}")
    }

  // ---

  private lazy val rejectionHandler = RejectionHandler
    .newBuilder()
    .handle {
      case UnsupportedRequestContentTypeRejection(supported) =>
        cors()(
          complete(
            StatusCodes.BadRequest,
            Json.obj(
              "error" -> "requestContentType",
              "details" -> Json.obj("supported" -> supported)
            )
          )
        )

      case ValidationRejection(message, _) =>
        cors()(
          complete(
            StatusCodes.InternalServerError,
            Json.obj("error" -> "validation", "details" -> Json.parse(message))
          )
        )

      case rej =>
        sys.error(s"${rej.getClass}: $rej")
    }
    .result()
}
