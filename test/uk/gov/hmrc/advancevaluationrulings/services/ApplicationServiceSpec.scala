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

import org.mockito.ArgumentMatchers.any
import org.mockito.ArgumentMatchersSugar.eqTo
import org.mockito.MockitoSugar
import org.scalatest.BeforeAndAfterEach
import org.scalatest.concurrent.{IntegrationPatience, ScalaFutures}
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers
import play.api.test.FakeRequest
import uk.gov.hmrc.advancevaluationrulings.models.Done
import uk.gov.hmrc.advancevaluationrulings.models.application._
import uk.gov.hmrc.advancevaluationrulings.repositories.{ApplicationRepository, CounterRepository}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.objectstore.client.Path
import uk.gov.hmrc.advancevaluationrulings.controllers.actions.IdentifierRequest
import uk.gov.hmrc.advancevaluationrulings.models.audit.ApplicationSubmissionEvent
import uk.gov.hmrc.auth.core.{AffinityGroup, Assistant}
import uk.gov.hmrc.advancevaluationrulings.models.audit.AuditMetadata

import java.time.{Clock, Instant, ZoneId}
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class ApplicationServiceSpec extends AnyFreeSpec with Matchers with MockitoSugar with BeforeAndAfterEach with ScalaFutures with IntegrationPatience {

  private val mockCounterRepo = mock[CounterRepository]
  private val mockApplicationRepo = mock[ApplicationRepository]
  private val mockDmsSubmissionService = mock[DmsSubmissionService]
  private val mockSubmissionReferenceService = mock[SubmissionReferenceService]
  private val mockAttachmentsService = mock[AttachmentsService]
  private val mockAuditService = mock[AuditService]
  private val fixedInstant = Instant.now
  private val fixedClock = Clock.fixed(fixedInstant, ZoneId.systemDefault())
  private val trader = TraderDetail("eori", "name", "line1", None, None, "postcode", "GB", None)
  private val goodsDetails = GoodsDetails("name", "description", None, None, None)
  private val submissionReference = "submissionReference"
  private val method = MethodOne(None, None, None)
  private val contact = ContactDetails("name", "email", None)
  private val hc: HeaderCarrier = HeaderCarrier()

  private val service = new ApplicationService(
    mockCounterRepo,
    mockApplicationRepo,
    mockDmsSubmissionService,
    mockSubmissionReferenceService,
    mockAttachmentsService,
    mockAuditService,
    fixedClock
  )

  override def beforeEach(): Unit = {
    reset(
      mockCounterRepo,
      mockApplicationRepo,
      mockDmsSubmissionService,
      mockSubmissionReferenceService,
      mockAttachmentsService,
      mockAuditService
    )
    super.beforeEach()
  }

  "save" - {

    "must create an application and return its id" in {

      val id = 123L
      val applicantEori = "applicantEori"

      when(mockCounterRepo.nextId(eqTo(CounterId.ApplicationId))) thenReturn Future.successful(id)
      when(mockSubmissionReferenceService.random()).thenReturn(submissionReference)
      when(mockApplicationRepo.set(any())) thenReturn Future.successful(Done)
      when(mockDmsSubmissionService.submitApplication(any(), any())(any())).thenReturn(Future.successful(Done))

      val applicationRequest = ApplicationRequest(
        draftId = "foo",
        trader = trader,
        agent = None,
        contact = contact,
        goodsDetails = goodsDetails,
        requestedMethod = method,
        attachments = Nil,
      )

      val auditMetadata = AuditMetadata(
        internalId = "internalId",
        affinityGroup = AffinityGroup.Organisation,
        credentialRole = Some(Assistant)
      )

      val expectedApplication = Application(
        id = ApplicationId(id),
        applicantEori = applicantEori,
        trader = trader,
        agent = None,
        contact = contact,
        goodsDetails = goodsDetails,
        requestedMethod = method,
        attachments = Nil,
        submissionReference = submissionReference,
        created = fixedInstant,
        lastUpdated = fixedInstant
      )

      val expectedAudit = ApplicationSubmissionEvent(
        internalId = "internalId",
        affinityGroup = AffinityGroup.Organisation,
        credentialRole = Some(Assistant),
        application = expectedApplication
      )

      val result = service.save(applicantEori, applicationRequest, auditMetadata)(hc).futureValue

      result mustEqual ApplicationId(id)
      verify(mockCounterRepo, times(1)).nextId(eqTo(CounterId.ApplicationId))
      verify(mockApplicationRepo, times(1)).set(eqTo(expectedApplication))
      verify(mockDmsSubmissionService, times(1)).submitApplication(eqTo(expectedApplication), any())(any())
      verify(mockAuditService, times(1)).auditSubmitRequest(eqTo(expectedAudit))(any())
    }

    "must convert attachments, giving them unique ids and copying the contents from the frontend" in {

      val id = 123L
      val applicantEori = "applicantEori"

      val attachmentRequest1 = AttachmentRequest("name1", None, "url1", Privacy.Public, "application/pdf", 1L)
      val attachmentRequest2 = AttachmentRequest("name2", None, "url2", Privacy.Public, "image/jpeg", 2L)
      val attachmentId1 = 1L
      val attachmentId2 = 2L

      when(mockCounterRepo.nextId(eqTo(CounterId.ApplicationId))).thenReturn(Future.successful(id))
      when(mockCounterRepo.nextId(eqTo(CounterId.AttachmentId))).thenReturn(
        Future.successful(attachmentId1),
        Future.successful(attachmentId2)
      )
      when(mockSubmissionReferenceService.random()).thenReturn(submissionReference)
      when(mockApplicationRepo.set(any())).thenReturn(Future.successful(Done))
      when(mockAttachmentsService.copyAttachment(any(), any())(any()))
        .thenReturn(
          Future.successful(Path.File("attachments/applicationId/url1")),
          Future.successful(Path.File("attachments/applicationId/url2"))
        )
      when(mockDmsSubmissionService.submitApplication(any(), any())(any())).thenReturn(Future.successful(Done))

      val applicationRequest = ApplicationRequest(
        draftId = "foo",
        trader = trader,
        agent = None,
        contact = contact,
        goodsDetails = goodsDetails,
        requestedMethod = method,
        attachments = Seq(attachmentRequest1, attachmentRequest2),
      )

      val auditMetadata = AuditMetadata(
        internalId = "internalId",
        affinityGroup = AffinityGroup.Organisation,
        credentialRole = Some(Assistant)
      )

      val expectedApplication = Application(
        id = ApplicationId(id),
        applicantEori = applicantEori,
        trader = trader,
        agent = None,
        contact = contact,
        goodsDetails = goodsDetails,
        requestedMethod = method,
        attachments = Seq(
          Attachment(attachmentId1, "name1", None, "attachments/applicationId/url1", Privacy.Public, "application/pdf", 1L),
          Attachment(attachmentId2, "name2", None, "attachments/applicationId/url2", Privacy.Public, "image/jpeg", 2L)
        ),
        submissionReference = submissionReference,
        created = fixedInstant,
        lastUpdated = fixedInstant
      )

      val result = service.save(applicantEori, applicationRequest, auditMetadata)(hc).futureValue

      result mustEqual ApplicationId(id)
      verify(mockCounterRepo, times(1)).nextId(eqTo(CounterId.ApplicationId))
      verify(mockApplicationRepo, times(1)).set(eqTo(expectedApplication))
      verify(mockCounterRepo, times(2)).nextId(eqTo(CounterId.AttachmentId))
      verify(mockAttachmentsService, times(1)).copyAttachment(eqTo(ApplicationId(id)), eqTo("url1"))(any())
      verify(mockAttachmentsService, times(1)).copyAttachment(eqTo(ApplicationId(id)), eqTo("url2"))(any())
    }
  }
}
