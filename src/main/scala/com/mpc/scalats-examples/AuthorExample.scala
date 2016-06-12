package com.mpc.scalats

import java.time.{Instant, LocalDate}

import com.mpc.scalats.core.TypeScriptGenerator

case class BookDto(title: String, pageCount: Int)

case class AddressDto(street: String,
                      city: String)

case class AuthorDto(id: String,
                     name: String,
                     age: Int,
                     address: AddressDto,
                     nicknames: List[String],
                     workAddress: AddressDto,
                     principal: AuthorDto,
                     books: List[BookDto],
                     creationDate: Instant,
                     birthday: LocalDate)


object AuthorExample {

  def main(args: Array[String]) {
    TypeScriptGenerator.generateFromClassNames(List("com.mpc.scalats.AuthorDto"), Console.out)
  }

}
