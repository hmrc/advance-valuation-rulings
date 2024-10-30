/*
 * Copyright 2024 HM Revenue & Customs
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

import scala.concurrent.ExecutionContext

import play.api.libs.json.Json
import play.api.mvc.{Action, AnyContent, ControllerComponents}
import uk.gov.hmrc.advancevaluationrulings.controllers.actions.IdentifierAction
import uk.gov.hmrc.advancevaluationrulings.services.TraderDetailsService
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController

@Singleton()
class TraderDetailsController @Inject() (
  cc: ControllerComponents,
  traderDetailsService: TraderDetailsService,
  identify: IdentifierAction
) extends BackendController(cc) {

  implicit val ec: ExecutionContext = cc.executionContext

  def retrieveTraderDetails(
    acknowledgementReference: String,
    eoriNumber: String
  ): Action[AnyContent] =
    identify.async { implicit request =>
      traderDetailsService
        .getTraderDetails(acknowledgementReference, eoriNumber)
        .map {
          case None                        => NotFound
          case Some(traderDetailsResponse) => Ok(Json.toJson(traderDetailsResponse))
        }

    }

}
