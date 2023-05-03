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

import java.time.{Clock, Instant, ZoneId}

import scala.concurrent.Future

import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.Json
import play.api.test.FakeRequest
import play.api.test.Helpers._
import uk.gov.hmrc.advancevaluationrulings.models.DraftId
import uk.gov.hmrc.advancevaluationrulings.models.application._
import uk.gov.hmrc.advancevaluationrulings.models.audit.AuditMetadata
import uk.gov.hmrc.advancevaluationrulings.repositories.ApplicationRepository
import uk.gov.hmrc.advancevaluationrulings.services.ApplicationService
import uk.gov.hmrc.auth.core._
import uk.gov.hmrc.auth.core.retrieve.~

import generators.ModelGenerators
import org.mockito.ArgumentMatchers.any
import org.mockito.ArgumentMatchersSugar.eqTo
import org.mockito.MockitoSugar
import org.scalatest.{BeforeAndAfterEach, OptionValues}
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers

class ApplicationControllerSpec
    extends AnyFreeSpec
    with Matchers
    with OptionValues
    with ModelGenerators
    with MockitoSugar
    with BeforeAndAfterEach {

  private val fixedClock                = Clock.fixed(Instant.now, ZoneId.systemDefault)
  private val mockApplicationService    = mock[ApplicationService]
  private val mockApplicationRepository = mock[ApplicationRepository]
  private val mockAuthConnector         = mock[AuthConnector]

  private val applicantEori       = "applicantEori"
  private val atarEnrolment       = Enrolments(
    Set(
      Enrolment("HMRC-ATAR-ORG", Seq(EnrolmentIdentifier("EORINumber", applicantEori)), "Activated")
    )
  )
  private val trader              = TraderDetail("eori", "name", "line1", None, None, "postcode", "GB", None)
  private val goodsDetails        = GoodsDetails("name", "description", None, None, None)
  private val submissionReference = "submissionReference"
  private val method              = MethodOne(None, None, None)
  private val contact             = ContactDetails("name", "email", None)

  private val app =
    GuiceApplicationBuilder()
      .overrides(
        bind[Clock].toInstance(fixedClock),
        bind[ApplicationService].toInstance(mockApplicationService),
        bind[ApplicationRepository].toInstance(mockApplicationRepository),
        bind[AuthConnector].toInstance(mockAuthConnector)
      )
      .build()

  override def beforeEach(): Unit = {
    reset(mockApplicationService, mockApplicationRepository, mockAuthConnector)
    super.beforeEach()
  }

  ".submit" - {

    "must save the application and return an application submission response" in {

      val id = 123L

      when(
        mockAuthConnector
          .authorise[Enrolments ~ Option[String] ~ Option[AffinityGroup] ~ Option[CredentialRole]](
            any(),
            any()
          )(any(), any())
      )
        .thenReturn(
          Future.successful(
            new ~(
              new ~(new ~(atarEnrolment, Some("internalId")), Some(AffinityGroup.Organisation)),
              Some(Assistant)
            )
          )
        )
      when(mockApplicationService.save(any(), any(), any())(any())) thenReturn Future.successful(
        ApplicationId(id)
      )

      val expectedMetadata = AuditMetadata(
        internalId = "internalId",
        affinityGroup = AffinityGroup.Organisation,
        credentialRole = Some(Assistant)
      )

      val applicationRequest = ApplicationRequest(
        draftId = DraftId(0),
        trader = trader,
        agent = None,
        contact = contact,
        goodsDetails = goodsDetails,
        requestedMethod = method,
        attachments = Nil
      )

      val request =
        FakeRequest(POST, routes.ApplicationController.submit.url)
          .withBody(Json.toJson(applicationRequest))

      val result = route(app, request).value

      status(result) mustEqual OK
      contentAsJson(result) mustEqual Json.toJson(ApplicationSubmissionResponse(ApplicationId(id)))
      verify(mockApplicationService, times(1)).save(
        eqTo(applicantEori),
        eqTo(applicationRequest),
        eqTo(expectedMetadata)
      )(any())
    }
  }

  ".summaries" - {

    "must return a list of application summaries" in {

      val summary = ApplicationSummary(ApplicationId(1), "name", Instant.now(fixedClock), "eori")

      when(
        mockAuthConnector
          .authorise[Enrolments ~ Option[String] ~ Option[AffinityGroup] ~ Option[CredentialRole]](
            any(),
            any()
          )(any(), any())
      )
        .thenReturn(
          Future.successful(
            new ~(
              new ~(new ~(atarEnrolment, Some("internalId")), Some(AffinityGroup.Organisation)),
              Some(Assistant)
            )
          )
        )
      when(mockApplicationRepository.summaries(any())).thenReturn(Future.successful(Seq(summary)))

      val request = FakeRequest(GET, routes.ApplicationController.summaries.url)
      val result  = route(app, request).value

      status(result) mustEqual OK
      contentAsJson(result) mustEqual Json.toJson(ApplicationSummaryResponse(Seq(summary)))
    }
  }

  ".get" - {

    "must return Ok when an application can be found" in {

      val applicationId = applicationIdGen.sample.value
      val application   = Application(
        id = applicationId,
        applicantEori = applicantEori,
        trader = trader,
        agent = None,
        contact = contact,
        goodsDetails = goodsDetails,
        requestedMethod = method,
        attachments = Nil,
        submissionReference = submissionReference,
        created = Instant.now(fixedClock),
        lastUpdated = Instant.now(fixedClock)
      )

      when(
        mockAuthConnector
          .authorise[Enrolments ~ Option[String] ~ Option[AffinityGroup] ~ Option[CredentialRole]](
            any(),
            any()
          )(any(), any())
      )
        .thenReturn(
          Future.successful(
            new ~(
              new ~(new ~(atarEnrolment, Some("internalId")), Some(AffinityGroup.Organisation)),
              Some(Assistant)
            )
          )
        )
      when(mockApplicationRepository.get(any(), any()))
        .thenReturn(Future.successful(Some(application)))

      val request = FakeRequest(GET, routes.ApplicationController.get(applicationId).url)
      val result  = route(app, request).value

      status(result) mustEqual OK
      contentAsJson(result) mustEqual Json.toJson(application)
    }

    "must return NotFound when an application cannot be found" in {

      val applicationId = applicationIdGen.sample.value

      when(
        mockAuthConnector
          .authorise[Enrolments ~ Option[String] ~ Option[AffinityGroup] ~ Option[CredentialRole]](
            any(),
            any()
          )(any(), any())
      )
        .thenReturn(
          Future.successful(
            new ~(
              new ~(new ~(atarEnrolment, Some("internalId")), Some(AffinityGroup.Organisation)),
              Some(Assistant)
            )
          )
        )
      when(mockApplicationRepository.get(any(), any())).thenReturn(Future.successful(None))

      val request = FakeRequest(GET, routes.ApplicationController.get(applicationId).url)
      val result  = route(app, request).value

      status(result) mustEqual NOT_FOUND
    }
  }
}
