package com.mpc.scalats

import java.time.{Instant, LocalDate}
import java.util.UUID

import com.mpc.scalats.configuration.Config
import com.mpc.scalats.core.{ Logger, TypeScriptGenerator }

case class NV(temp: Boolean)

case class SV(a: Int, b: Double, c: Int)

object JTypes {
  type PSV = Either[SV, NV]
}

case class BookDto(title: String, pageCount: Int)

case class AddressDto(street: String,
                      city: String)

case class Both(s: Either[BookDto, AddressDto])

case class Wrapper(s: String) extends AnyVal

case class AuthorDto(id: UUID,
                     name: String,
                     b: Both,
                     c: Wrapper,
                     d: JTypes.PSV,
                     age: Option[Int],
                     mapTest: Map[AddressDto, Option[Int]],
                     address: Either[AddressDto, Option[Int]],
                     nicknames: List[String],
                     workAddress: Option[AddressDto],
                     principal: AuthorDto,
                     books: List[Option[BookDto]],
                     creationDate: Instant,
                     birthday: LocalDate,
                     isRetired: Boolean)

object AuthorExample {
  def main(args: Array[String]): Unit = {
    val logger = Logger(
      org.slf4j.LoggerFactory getLogger TypeScriptGenerator.getClass)

    TypeScriptGenerator.generateFromClassNames(
      List("com.mpc.scalats.AuthorDto"),
      logger)(Config())
  }

}
