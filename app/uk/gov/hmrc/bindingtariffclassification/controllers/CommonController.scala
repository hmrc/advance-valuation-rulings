/*
 * Copyright 2021 HM Revenue & Customs
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

package uk.gov.hmrc.bindingtariffclassification.controllers

import play.api.libs.json._
import play.api.mvc.{MessagesControllerComponents, Request, Result}
import uk.gov.hmrc.bindingtariffclassification.common.Logging
import uk.gov.hmrc.bindingtariffclassification.model.ErrorCode._
import uk.gov.hmrc.bindingtariffclassification.model.JsErrorResponse
import uk.gov.hmrc.play.bootstrap.controller.BackendController

import scala.concurrent.Future
import scala.concurrent.Future.successful
import scala.util.{Failure, Success, Try}

class CommonController(
  mcc: MessagesControllerComponents
) extends BackendController(mcc)
    with Logging {

  override protected def withJsonBody[T](
    f: T => Future[Result]
  )(implicit request: Request[JsValue], m: Manifest[T], reads: Reads[T]): Future[Result] =
    Try(request.body.validate[T]) match {
      case Success(JsSuccess(payload, _)) => f(payload)
      case Success(JsError(errs)) => {
        logger.debug(s"JSON deserialisation failure : ${JsError.toJson(errs)}")
        successful(BadRequest(JsErrorResponse(INVALID_REQUEST_PAYLOAD, JsError.toJson(errs))))
      }
      case Failure(e) => successful(BadRequest(JsErrorResponse(UNKNOWN_ERROR, e.getMessage)))
    }

  private[controllers] def recovery: PartialFunction[Throwable, Result] = {
    case e: Throwable => handleException(e)
  }

  private[controllers] def handleException(e: Throwable) = {
    logger.error(s"An unexpected error occurred: ${e.getMessage}", e)
    InternalServerError(JsErrorResponse(UNKNOWN_ERROR, "An unexpected error occurred"))
  }

}
