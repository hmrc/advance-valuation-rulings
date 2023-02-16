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

import javax.inject.{Inject, Singleton}

import scala.concurrent.{ExecutionContext, Future}

import play.api.libs.json.JsValue
import play.api.mvc.{Action, ControllerComponents, Result}
import uk.gov.hmrc.advancevaluationrulings.models.traderdetails.{TraderDetailsRequest, TraderDetailsResponse}
import uk.gov.hmrc.advancevaluationrulings.services.TraderDetailsService
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController

@Singleton()
class ARSController @Inject() (
  cc: ControllerComponents,
  traderDetailsService: TraderDetailsService
) extends BackendController(cc)
    with BaseController {

  implicit val ec: ExecutionContext = cc.executionContext

  def retrieveTraderDetails(): Action[JsValue] =
    Action.async(parse.json) {
      implicit request =>
        extractFromJson[TraderDetailsRequest] {
          traderDetailsRequest =>
            createResponse[TraderDetailsResponse](200) {
              import traderDetailsRequest._
              traderDetailsService.getTraderDetails(
                date,
                acknowledgementReference,
                taxPayerID,
                EORI
              )
            }
        }
    }

}
