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

package uk.gov.hmrc.advancevaluationrulings.services

import cats.implicits._
import uk.gov.hmrc.advancevaluationrulings.controllers.actions.IdentifierRequest
import uk.gov.hmrc.advancevaluationrulings.models.application._
import uk.gov.hmrc.advancevaluationrulings.models.audit.ApplicationSubmissionEvent
import uk.gov.hmrc.advancevaluationrulings.repositories.{ApplicationRepository, CounterRepository}
import uk.gov.hmrc.http.HeaderCarrier

import java.time.{Clock, Instant}
import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class ApplicationService @Inject()(
                                    counterRepository: CounterRepository,
                                    applicationRepository: ApplicationRepository,
                                    dmsSubmissionService: DmsSubmissionService,
                                    submissionReferenceService: SubmissionReferenceService,
                                    attachmentsService: AttachmentsService,
                                    auditService: AuditService,
                                    clock: Clock
                                  )(implicit ec: ExecutionContext) {

  def save(request: IdentifierRequest[ApplicationRequest])(implicit hc: HeaderCarrier): Future[ApplicationId] =
    for {
      appId               <- counterRepository.nextId(CounterId.ApplicationId).map(ApplicationId(_))
      attachments         <- buildAttachments(appId, request.body.attachments)
      submissionReference =  submissionReferenceService.random()
      application         <- saveApplication(request.eori, request.body, appId, attachments, submissionReference)
      _                   <- dmsSubmissionService.submitApplication(application, submissionReference)
      event               =  ApplicationSubmissionEvent(request.internalId, request.affinityGroup, request.credentialRole, application)
      _                   =  auditService.auditSubmitRequest(event)
    } yield appId

  private def saveApplication(applicantEori: String, request: ApplicationRequest, appId: ApplicationId, attachments: Seq[Attachment], submissionReference: String): Future[Application] = {

    val application = Application(
      id = appId,
      applicantEori = applicantEori,
      trader = request.trader,
      agent = request.agent,
      contact = request.contact,
      goodsDetails = request.goodsDetails,
      requestedMethod = request.requestedMethod,
      attachments = attachments,
      submissionReference = submissionReference,
      created = Instant.now(clock),
      lastUpdated = Instant.now(clock)
    )

    applicationRepository.set(application).as(application)
  }

  private def buildAttachments(applicationId: ApplicationId, requests: Seq[AttachmentRequest])(implicit hc: HeaderCarrier): Future[Seq[Attachment]] =
    requests.traverse { request =>
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
    }
}
