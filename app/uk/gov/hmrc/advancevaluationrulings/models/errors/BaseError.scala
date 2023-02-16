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

abstract class BaseError(statusCode: Int, description: String) extends Product with Serializable

object BaseError {
  implicit class BaseErrorExt(error: BaseError) {
    def toErrorResponse: ErrorResponse =
      error match {
        case ConnectorError(description) =>
          ErrorResponse(statusCode = 500, ValidationErrors(Seq(ValidationError(description))))
        case e =>
          ErrorResponse(statusCode = 500, ValidationErrors(Seq(ValidationError(e.toString))))
      }
  }
}
