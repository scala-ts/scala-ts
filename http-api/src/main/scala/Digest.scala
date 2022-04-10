package io.github.scalats.demo

/** Common functions */
private[demo] object Digest {
  private val HEX_CHARS: Array[Char] = "0123456789abcdef".toCharArray

  /** Turns an array of Byte into a String representation in hexadecimal. */
  @com.github.ghik.silencer.silent("\\+=\\ in\\ trait\\ Growable")
  def hex2Str(bytes: Array[Byte]): String = {
    val len = bytes.length
    val hex = new Array[Char](2 * len)

    var i = 0
    while (i < len) {
      val b = bytes(i)

      val t = 2 * i // index in output buffer

      hex(t) = HEX_CHARS((b & 0xf0) >>> 4)
      hex(t + 1) = HEX_CHARS(b & 0x0f)

      i = i + 1
    }

    new String(hex)
  }

  /** Returns the MD5 hash for the given `string`, and turns it into a
    * hexadecimal String representation.
    *
    * @param string
    *   the string to be hashed
    * @param encoding
    *   the string encoding/charset
    */
  def md5Hex(string: String, encoding: String): String =
    hex2Str(md5(string, encoding))

  /** Returns the MD5 hash of the given `string`.
    *
    * @param string
    *   the string to be hashed
    * @param encoding
    *   the string encoding/charset
    */
  def md5(string: String, encoding: String): Array[Byte] =
    md5(string.getBytes(encoding))

  /** Computes the MD5 hash of the given `bytes`. */
  def md5(bytes: Array[Byte]): Array[Byte] =
    java.security.MessageDigest.getInstance("MD5").digest(bytes)
}
