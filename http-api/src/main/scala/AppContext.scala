package io.github.scalats.demo

import scala.util.Try
import scala.util.control.NonFatal

import scala.concurrent.duration._

import akka.actor.ActorSystem
import akka.stream.Materializer
import com.typesafe.config.Config
import org.slf4j.LoggerFactory

final class AppContext(
    val name: String,
    val retries: Int,
    val startupTimeout: Duration,
    val httpPort: Int,
    val httpIf: String) {
  implicit val system = ActorSystem(name)
  implicit val executor = system.dispatcher
  implicit val materializer = Materializer(system)

  val logger = LoggerFactory.getLogger(name)

  @volatile private var stopped = false

  def close(): Unit =
    if (stopped) {
      logger.debug(s"${name} already stopped.")
    } else {
      logger.info(s"Stopping ${name}...")

      try {
        // cleanup
        // eg. close connectors like mongodb, ws, etc.

        logger.info("Successfully stopped")
      } catch {
        case NonFatal(cause) =>
          logger.error("Failed to close session", cause)
      }

      system.terminate()

      stopped = true
    }

  override def toString: String =
    s"{name: $name, retries: $retries: http: {port: $httpPort, interface: $httpIf}}"
}

object AppContext {
  val logger = LoggerFactory.getLogger(getClass)

  def apply(config: Config): Try[AppContext] = for {
    retries <- Try(config getInt "startup.retries")
    startupTimeout <- Try(config getInt "startup.timeout").map(_.seconds)
    name <- Try(config getString "appName")

    // HTTP binding
    httpPort <- Try(config getInt "http.port")
    httpIf <- Try(config getString "http.interface")
  } yield {
    // instanciate services like kafka, mongodb
    // val kafka = new Service(kConf)

    new AppContext(name, retries, startupTimeout, httpPort, httpIf)
  }
}
