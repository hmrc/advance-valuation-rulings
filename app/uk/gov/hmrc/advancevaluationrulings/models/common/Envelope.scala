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

package uk.gov.hmrc.advancevaluationrulings.models.common

import scala.concurrent.{ExecutionContext, Future}

import play.api.libs.json.{Format, Json}
import play.api.libs.json.OFormat.oFormatFromReadsAndOWrites
import play.api.mvc.{Result, Results}
import uk.gov.hmrc.advancevaluationrulings.models.errors.BaseError
import uk.gov.hmrc.http.HeaderCarrier

import cats.data.EitherT
import cats.implicits._

object Envelope {

  type Envelope[T] = EitherT[Future, BaseError, T]

  def apply[T](future: Future[Either[BaseError, T]]): EitherT[Future, BaseError, T] = {
    EitherT.apply(future)
  }

  implicit class EnvelopeExt[T](envelope: EitherT[Future, BaseError, T]) {
    def toResult(implicit
      ec: ExecutionContext,
      hc: HeaderCarrier,
      format: Format[T]
    ): Future[Result] =
      envelope
        .leftMap(_.toErrorResponse)
        .fold(
          error => Results.Status(error.statusCode)(Json.toJson(error)),
          success => Results.Status(200)(Json.toJson(success))
        )
  }

}
