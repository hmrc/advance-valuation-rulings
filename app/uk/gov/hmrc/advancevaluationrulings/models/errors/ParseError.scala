/*
 * Copyright 2023 HM Revenue & Customs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package uk.gov.hmrc.advancevaluationrulings.models.errors

import scala.collection.Seq

import play.api.libs.json.{JsonValidationError, JsPath}
import uk.gov.hmrc.advancevaluationrulings.models.errors.ParseError._

import cats.Show
import cats.implicits._

case class ParseError(errors: ParseErrors)
    extends ReaderError(description = s"Failed to parse json response. Error: ${errors.show}")

object ParseError {
  type ParseErrors = Seq[(JsPath, Seq[JsonValidationError])]

  implicit val showPath: Show[ParseErrors] = Show.show {
    errors =>
      errors
        .map {
          error =>
            val (jsPath, validationErrors) = error
            s"$jsPath: ${validationErrors.map(_.message).mkString(", ")}"
        }
        .mkString(", ")
  }
}