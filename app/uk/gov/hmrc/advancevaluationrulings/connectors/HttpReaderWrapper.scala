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

package uk.gov.hmrc.advancevaluationrulings.connectors

import scala.util.{Failure, Success, Try}

import play.api.http.Status
import play.api.libs.json.{Json, JsValue, Reads}
import uk.gov.hmrc.advancevaluationrulings.logging.RequestAwareLogger
import uk.gov.hmrc.advancevaluationrulings.models.common.Envelope.Envelope
import uk.gov.hmrc.advancevaluationrulings.models.errors.{BaseError, JsonSerializationError, ParseError}
import uk.gov.hmrc.http.{HeaderCarrier, HttpReads, HttpResponse}

import cats.implicits._

trait HttpReaderWrapper[T, E <: BaseError] {

  protected lazy val logger: RequestAwareLogger = new RequestAwareLogger(this.getClass)

  def withHttpReader(
    func: HttpReads[Either[BaseError, T]] => Envelope[T]
  )(implicit
    headerCarrier: HeaderCarrier,
    successReader: Reads[T],
    errorReader: Reads[E]
  ): Envelope[T] =
    func {
      (_, _, response) =>
        logger.debug(s"Got response status code: ${response.status}")
        if (Status.isSuccessful(response.status)) {
          readResponse(response)(
            responseJson =>
              responseJson.validate[T].fold(errors => ParseError(errors).asLeft[T], Right(_))
          )
        } else {
          readResponse(response)(
            responseJson =>
              responseJson.validate[E].fold(errors => ParseError(errors).asLeft[T], Left(_))
          )
        }
    }

  private def readResponse(
    response: HttpResponse
  )(successHandler: JsValue => Either[BaseError, T])(implicit headerCarrier: HeaderCarrier) =
    Try(response.json) match {
      case Success(validJson) =>
        logger.debug(s"Got response: ${Json.prettyPrint(validJson)}")
        successHandler(validJson)
      case Failure(exception) =>
        logger.error(
          s"Failed to serialize upstream json response: ${response.body}\n ${exception.getMessage}"
        )
        JsonSerializationError(exception).asLeft[T]
    }

}
