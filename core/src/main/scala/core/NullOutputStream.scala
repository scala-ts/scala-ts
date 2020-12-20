package io.github.scalats.core

/** Output stream that ignore every pushed data. */
sealed class NullOutputStream extends java.io.OutputStream {
  override def close(): Unit = ()
  override def flush(): Unit = ()
  override def write(b: Array[Byte]): Unit = ()
  override def write(b: Array[Byte], off: Int, len: Int): Unit = ()

  @SuppressWarnings(Array("EmptyMethod"))
  def write(b: Int): Unit = { () }
}

object NullOutputStream extends NullOutputStream
