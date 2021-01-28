package io.github.scalats.demo

import scala.util.{ Failure, Success, Try }
import scala.concurrent.Await

import akka.http.scaladsl.Http
import akka.http.scaladsl.server.Route

object Runner {
  def apply(appCtx: AppContext): Try[Router] = {
    import appCtx.{ httpIf, httpPort, logger, startupTimeout }

    val router = new Router(appCtx)
    val handler = {
      import appCtx.system

      Route.toFunction(router.instance)
    }

    Try {
      import appCtx.executor

      Await.result(
        Http(appCtx.system)
          .newServerAt(httpIf, httpPort)
          .bind(handler)
          .andThen {
            case Success(_) =>
              logger.info(s"${appCtx.name} is up @ ${httpIf}:${httpPort}")

            case Failure(cause) =>
              logger.warn(s"Fails to start ${appCtx.name}", cause)
          }
          .map(_ => router),
        startupTimeout
      )
    }
  }
}
