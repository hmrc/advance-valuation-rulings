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
import uk.gov.hmrc.advancevaluationrulings.controllers.ValuationCaseController.{AssignCaseRequest, CreateValuationRequest, RejectCaseRequest}
import uk.gov.hmrc.advancevaluationrulings.models.{Attachment, CaseWorker, RejectReason, ValuationApplication}
import uk.gov.hmrc.advancevaluationrulings.services.ValuationCaseService
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

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

  def allNewCases: Action[AnyContent] = Action.async { request =>
    for {
      cases <- valuationCaseService.allNewCases
    } yield Ok(Json.toJson(cases))
  }

  def findByReference(reference: String): Action[AnyContent] = Action.async{ request =>
    for {
      c <- valuationCaseService.findByReference(reference)
    } yield Ok(Json.toJson(c))
  }

  def findByAssignee(assignee: String): Action[AnyContent] = Action.async{ request =>
    for {
      cases <- valuationCaseService.findByAssignee(assignee)
    } yield Ok(Json.toJson(cases))
  }

  def assignCase: Action[AssignCaseRequest] = Action.async(parse.json[AssignCaseRequest]) { request =>
    for {
      c <- valuationCaseService.assignCase(request.body.reference, request.body.caseWorker)
    } yield Ok(Json.toJson(c))
  }

  def rejectCase: Action[RejectCaseRequest] = Action.async(parse.json[RejectCaseRequest]) { request =>
    val rejection = request.body
    for {
      c <- valuationCaseService.rejectCase(rejection.reference, rejection.reason, rejection.attachment,rejection.note)
    } yield Ok(Json.toJson(c))
  }

  def unAssignCase: Action[AssignCaseRequest] = Action.async(parse.json[AssignCaseRequest]) { request =>
    for {
      c <- valuationCaseService.unAssignCase(request.body.reference, request.body.caseWorker)
    } yield Ok(Json.toJson(c))
  }

}

object ValuationCaseController {

  case class CreateValuationRequest(reference: String, valuation: ValuationApplication)

  object CreateValuationRequest{
    implicit val fmt: OFormat[CreateValuationRequest] = Json.format[CreateValuationRequest]
  }

  case class AssignCaseRequest(reference: String, caseWorker: CaseWorker)

  object AssignCaseRequest {
    implicit val fmt: OFormat[AssignCaseRequest] = Json.format[AssignCaseRequest]
  }

  case class RejectCaseRequest(reference: String, reason: RejectReason.Value, attachment: Attachment, note: String)

  object RejectCaseRequest {
    implicit val fmt: OFormat[RejectCaseRequest] = Json.format[RejectCaseRequest]
  }
}

