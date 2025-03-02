/*
 * Copyright 2025 HM Revenue & Customs
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

package uk.gov.hmrc.advancevaluationrulings.services

import cats.implicits._
import uk.gov.hmrc.advancevaluationrulings.models.DraftId
import uk.gov.hmrc.advancevaluationrulings.models.application._
import uk.gov.hmrc.advancevaluationrulings.models.audit.{ApplicationSubmissionEvent, AuditMetadata}
import uk.gov.hmrc.advancevaluationrulings.repositories.{ApplicationRepository, CounterRepository}
import uk.gov.hmrc.http.HeaderCarrier

import java.time.{Clock, Instant}
import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class ApplicationService @Inject() (
  counterRepository: CounterRepository,
  applicationRepository: ApplicationRepository,
  dmsSubmissionService: DmsSubmissionService,
  submissionReferenceService: SubmissionReferenceService,
  attachmentsService: AttachmentsService,
  auditService: AuditService,
  clock: Clock
)(implicit ec: ExecutionContext) {

  def save(eori: String, request: ApplicationRequest, auditMetadata: AuditMetadata)(implicit
    hc: HeaderCarrier
  ): Future[ApplicationId] =
    for {
      appId              <- counterRepository.nextId(CounterId.ApplicationId).map(ApplicationId(_))
      attachments        <- buildAttachments(appId, request.attachments)
      submissionReference = submissionReferenceService.random()
      letterOfAuthority  <- buildLetterOfAuthority(appId, request.letterOfAuthority)
      application        <- saveApplication(
                              eori,
                              request,
                              appId,
                              attachments,
                              letterOfAuthority,
                              submissionReference
                            )
      _                  <- dmsSubmissionService.submitApplication(application, submissionReference)
      _                   = auditService.auditSubmitRequest(buildAudit(application, auditMetadata, request.draftId))
    } yield appId

  private def saveApplication(
    applicantEori: String,
    request: ApplicationRequest,
    appId: ApplicationId,
    attachments: Seq[Attachment],
    letterOfAuthority: Option[Attachment],
    submissionReference: String
  ): Future[Application] = {

    val application = Application(
      id = appId,
      applicantEori = applicantEori,
      trader = request.trader,
      agent = request.agent,
      contact = request.contact,
      goodsDetails = request.goodsDetails,
      requestedMethod = request.requestedMethod,
      attachments = attachments,
      whatIsYourRoleResponse = Some(request.whatIsYourRole),
      submissionReference = submissionReference,
      letterOfAuthority = letterOfAuthority,
      created = Instant.now(clock),
      lastUpdated = Instant.now(clock)
    )

    applicationRepository.set(application).as(application)
  }

  private def buildAttachments(applicationId: ApplicationId, requests: Seq[AttachmentRequest])(implicit
    hc: HeaderCarrier
  ): Future[Seq[Attachment]] =
    requests.traverse(request => buildAttachment(applicationId, request))

  private def buildLetterOfAuthority(
    applicationId: ApplicationId,
    request: Option[AttachmentRequest]
  )(implicit
    hc: HeaderCarrier
  ): Future[Option[Attachment]] =
    request.traverse(buildAttachment(applicationId, _))

  private def buildAttachment(applicationId: ApplicationId, request: AttachmentRequest)(implicit
    hc: HeaderCarrier
  ) =
    for {
      id  <- counterRepository.nextId(CounterId.AttachmentId)
      url <- attachmentsService.copyAttachment(applicationId, request.url)
    } yield Attachment(
      id = id,
      name = request.name,
      description = request.description,
      location = url.asUri,
      privacy = request.privacy,
      mimeType = request.mimeType,
      size = request.size
    )

  private def buildAudit(
    application: Application,
    auditMetadata: AuditMetadata,
    draftId: DraftId
  ): ApplicationSubmissionEvent =
    ApplicationSubmissionEvent(
      internalId = auditMetadata.internalId,
      affinityGroup = auditMetadata.affinityGroup,
      credentialRole = auditMetadata.credentialRole,
      application = application,
      draftId = draftId
    )
}
