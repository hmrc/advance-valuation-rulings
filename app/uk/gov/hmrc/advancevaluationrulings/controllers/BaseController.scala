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

package uk.gov.hmrc.advancevaluationrulings.controllers

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success, Try}
import play.api.libs.json._
import play.api.mvc.{Request, Result, Results}
import uk.gov.hmrc.advancevaluationrulings.models.common.Envelope.Envelope
import uk.gov.hmrc.advancevaluationrulings.models.errors.{ValidationError, ValidationErrors}
import uk.gov.hmrc.http.HeaderCarrier

trait BaseController {

  def extractFromJson[T](func: T => Future[Result])(implicit request: Request[JsValue], reads: Reads[T]): Future[Result] =
    Try(request.body.validate[T]) match {
      case Success(JsSuccess(payload, _)) => func(payload)
      case Success(JsError(errors))       =>
        val failureReason = errors
          .map { case (path, _) => s"field at path: [$path] missing or invalid" }
          .mkString(", ")
        toBadRequest(failureReason)
      case Failure(e)                     =>
        toBadRequest(e.getMessage)
    }

  def createResponse[T](successStatusCode: Int)(result: => Envelope[T])(implicit
    ec: ExecutionContext,
    hc: HeaderCarrier,
    format: Format[T]
  ): Future[Result] =
    result
      .leftMap(_.toErrorResponse)
      .fold(
        error => Results.Status(error.statusCode)(Json.toJson(error)),
        success => Results.Status(successStatusCode)(Json.toJson(success))
      )

  private def toBadRequest(errorMessage: String) =
    Future.successful(
      Results.BadRequest(
        Json.toJson(ValidationErrors(Seq(ValidationError(errorMessage))))
      )
    )
}
