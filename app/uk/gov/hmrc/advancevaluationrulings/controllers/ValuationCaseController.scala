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

import play.api.libs.json.{Json, OFormat}
import play.api.mvc.{Action, AnyContent, ControllerComponents}
import uk.gov.hmrc.advancevaluationrulings.controllers.ValuationCaseController.CreateValuationRequest
import uk.gov.hmrc.advancevaluationrulings.models.{ValuationApplication}
import uk.gov.hmrc.advancevaluationrulings.services.ValuationCaseService
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController

import javax.inject.{Inject, Singleton}
import scala.concurrent.ExecutionContext

@Singleton()
class ValuationCaseController @Inject() (
                                          cc: ControllerComponents,
                                          valuationCaseService: ValuationCaseService
                                        )(implicit ec: ExecutionContext) extends BackendController(cc) {

  def create(): Action[CreateValuationRequest] = Action.async(parse.json[CreateValuationRequest]){ request =>
    for {
      cases <- valuationCaseService.create(request.body.reference, request.body.valuation)
    } yield Ok(Json.toJson(cases))
  }

  def allOpenCases: Action[AnyContent] = Action.async{ request =>
    for{
      cases <- valuationCaseService.allOpenCases
    } yield Ok(Json.toJson(cases))
  }
}

object ValuationCaseController {

  case class CreateValuationRequest(reference: String, valuation: ValuationApplication)

  object CreateValuationRequest{
    implicit val fmt: OFormat[CreateValuationRequest] = Json.format[CreateValuationRequest]
  }
}

