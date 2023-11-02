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

import play.api.libs.json.Json
import play.api.mvc.{Action, AnyContent, ControllerComponents, Result}
import uk.gov.hmrc.advancevaluationrulings.controllers.actions.{IdentifierAction, IdentifierRequest}
import uk.gov.hmrc.advancevaluationrulings.models.application._
import uk.gov.hmrc.advancevaluationrulings.models.audit.AuditMetadata
import uk.gov.hmrc.advancevaluationrulings.repositories.ApplicationRepository
import uk.gov.hmrc.advancevaluationrulings.services._
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class ApplicationController @Inject()(
                                       cc: ControllerComponents,
                                       applicationService: ApplicationService,
                                       applicationRepository: ApplicationRepository,
                                       dmsSubmissionService: DmsSubmissionService,
                                       submissionReferenceService: SubmissionReferenceService,
                                       attachmentsService: AttachmentsService,
                                       auditService: AuditService,
                                       identify: IdentifierAction
                                     )(implicit ec: ExecutionContext)
  extends BackendController(cc) {

  def submit: Action[ApplicationRequest] = identify(parse.json[ApplicationRequest]).async {
    implicit request =>
      val auditMetadata = getAuditMetadata(request)
      for {
        optApp <- applicationRepository.getBasedOnDraftId(request.body.draftId)
        application <- applicationService.save(request.eori, request.body, optApp)
        _ <- dmsSubmissionService.submitApplication(application, application.submissionReference)
        _ = auditService.auditSubmitRequest(applicationService.buildAudit(application, auditMetadata, request.body.draftId))
      } yield {
        Ok(Json.toJson(ApplicationSubmissionResponse(application.id)))
      }
  }

  def summaries: Action[AnyContent] = identify.async {
    implicit request =>
      applicationRepository
        .summaries(request.eori)
        .map(summaries => Ok(Json.toJson(ApplicationSummaryResponse(summaries))))
  }

  def get(applicationId: ApplicationId): Action[AnyContent] = identify.async {
    implicit request =>
      applicationRepository
        .get(applicationId, request.eori)
        .map {
          _.map {
            application =>
              val seralized: Result = seraliseApplicationToJSON(application)
              seralized
          }
            .getOrElse(NotFound)
        }
  }

  private def seraliseApplicationToJSON(application: Application) =
    Ok(Json.toJson(application))

  private def getAuditMetadata(request: IdentifierRequest[_]): AuditMetadata =
    AuditMetadata(
      internalId = request.internalId,
      affinityGroup = request.affinityGroup,
      credentialRole = request.credentialRole
    )
}
