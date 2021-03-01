package io.github.scalats.demo

import akka.http.scaladsl.model.{
  ContentTypeRange,
  HttpCharsetRange,
  MediaRange
}

import io.github.scalats.demo.model._

import play.api.libs.json._

/* JSON codecs to read Scala types from requests and write to responses. */
private[demo] object Codecs {
  implicit val usageReads: Reads[Usage.Usage] =
    Reads.enumNameReads(Usage)

  implicit val usageWrites: Writes[Usage.Usage] =
    Writes.enumNameWrites[Usage.type]

  implicit val contactNameFormat: OFormat[ContactName] = Json.format

  implicit val userNameFormat: Format[UserName] = Json.valueFormat

  implicit val foodReads: Reads[Food] = {
    val PizzaName = Pizza.name // Makes it stable

    Reads[Food] {
      // Could be implemented more easily
      // using https://github.com/lloydmeta/enumeratum

      case JsString(JapaneseSushi.name) =>
        JsSuccess(JapaneseSushi)

      case JsString(PizzaName) =>
        JsSuccess(Pizza)

      case JsString(name) =>
        JsSuccess(OtherFood(name))

      case obj @ JsObject(_) =>
        (obj \ "name").validate[String].map(OtherFood(_))

      case v =>
        JsError(JsonValidationError("error.food.invalid", v))
    }
  }

  implicit val foodWrites: Writes[Food] = Writes[Food] {
    case OtherFood(name) =>
      Json.obj("_type" -> "OtherFood", "name" -> name)

    case food =>
      Json.toJson(food.name)
  }

  implicit val accountFormat: OFormat[Account] = Json.format

  implicit val credentialsFormat: OFormat[Credentials] = Json.format

  implicit val authenticatedUserWrites: OWrites[AuthenticatedUser] = Json.format

  // ---

  implicit val contentTypeRangeWrites: Writes[ContentTypeRange] = {
    implicit val charsetRange = Writes[HttpCharsetRange] { range =>
      Json.toJson(range.qValue)
    }

    implicit val mediaRange = Writes[MediaRange] { range =>
      Json.toJson(range.value)
    }

    Json.writes
  }
}
