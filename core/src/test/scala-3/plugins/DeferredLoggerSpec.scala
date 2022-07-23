package io.github.scalats.plugins

final class DeferredLoggerSpec extends org.specs2.mutable.Specification {
  "Deferrer logger".title

  sequential

  "Logger" should {
    val out = Seq.newBuilder[String]

    object TestLogger extends io.github.scalats.core.Logger {
      def debug(msg: => String): Unit = out += s"DEBUG: $msg"
      def info(msg: => String): Unit = out += s"INFO: $msg"
      def warning(msg: => String): Unit = out += s"WARN: $msg"
    }

    val deferred = new DeferredLogger

    "bufferize" in {
      (
        deferred.debug("Foo"),
        deferred.info("Bar"),
        deferred.warning("Lorem")
      ) must_=== ({}, {}, {}) and {
        out.result() must_=== Seq.empty
      }
    }

    "output deferred entries" in {
      deferred(TestLogger) must_=== ({}) and {
        out.result() must_=== Seq(
          "DEBUG: Foo",
          "INFO: Bar",
          "WARN: Lorem"
        )
      } and {
        out.clear() must_=== ({})
      }
    }

    "delegate to applied logger" in {
      (
        deferred.debug("Ipsum"),
        deferred.info("Dolor"),
        deferred.warning("Bolo")
      ) must_=== ({}, {}, {}) and {
        out.result() must_=== Seq(
          "DEBUG: Ipsum",
          "INFO: Dolor",
          "WARN: Bolo"
        )
      }
    }
  }
}
