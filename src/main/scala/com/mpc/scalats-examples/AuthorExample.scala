package com.mpc.scalats

import java.time.{Instant, LocalDate}
import java.util.UUID

import com.mpc.scalats.configuration.Config
import com.mpc.scalats.core.TypeScriptGenerator

case class BookDto(title: String, pageCount: Int)

case class AddressDto(street: String,
                      city: String)

case class AuthorDto(id: UUID,
                     name: String,
                     age: Option[Int],
                     address: AddressDto,
                     nicknames: List[String],
                     workAddress: Option[AddressDto],
                     principal: AuthorDto,
                     books: List[Option[BookDto]],
                     creationDate: Instant,
                     birthday: LocalDate,
                     isRetired: Boolean)

object AuthorExample {

  def main(args: Array[String]) {
    TypeScriptGenerator.generateFromClassNames(List("com.mpc.scalats.AuthorDto"))(Config())
  }

}
