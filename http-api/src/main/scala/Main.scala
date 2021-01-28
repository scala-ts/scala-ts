package io.github.scalats.demo

import scala.util.Try
import scala.util.control.NonFatal

import com.typesafe.config.ConfigFactory

object Main extends App {
  private lazy val launcher = for {
    config <- Try(ConfigFactory.load())
    appCtx <- AppContext(config)

    _ /*http*/ <- {
      appCtx.logger.info(s"Starting ${appCtx.name}...")
      appCtx.logger.debug(s"Configuration: $appCtx")

      Runner(appCtx)
    }
  } yield register(appCtx)

  @SuppressWarnings(Array("TryGet"))
  private def launch() = launcher.get

  launch()

  // ---

  private def register(context: AppContext): Unit = {
    Runtime.getRuntime.addShutdownHook(new Thread() {
      override def run(): Unit = {
        context.logger.info(s"Stopping ${context.name}...")

        context.close()
      }
    })

    sys.props.get("stopOnEOF").foreach { _ =>
      import context.executor

      println("\r\nPress <Ctrl+D> to stop the application ...\r\n")

      scala.concurrent.Future {
        @SuppressWarnings(Array("WhileTrue", "SwallowedException"))
        def wait(): Unit = while (true) {
          try {
            scala.io.StdIn.readChar()
          } catch {
            case NonFatal(cause: java.io.EOFException) => // Ctrl+D = EOF
              throw cause // relay to onComplete

            case NonFatal(_) =>
            // ignore
          }
        }

        wait()
      }.onComplete { case _ => context.close() }
    }
  }
}
