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

import org.mockito.ArgumentMatchers.{any, eq => eqTo}
import org.mockito.Mockito._
import org.scalatest.BeforeAndAfterEach
import org.scalatest.freespec.AnyFreeSpec
import play.api
import play.api.inject.bind
import play.api.libs.json.Json
import play.api.test.FakeRequest
import play.api.test.Helpers._
import uk.gov.hmrc.advancevaluationrulings.base.SpecBase
import uk.gov.hmrc.advancevaluationrulings.generators.ModelGenerators
import uk.gov.hmrc.advancevaluationrulings.models.DraftId
import uk.gov.hmrc.advancevaluationrulings.models.application._
import uk.gov.hmrc.advancevaluationrulings.models.audit.AuditMetadata
import uk.gov.hmrc.advancevaluationrulings.services.ApplicationService
import uk.gov.hmrc.auth.core._
import uk.gov.hmrc.auth.core.retrieve.~

import java.time.{Clock, Instant, ZoneId}
import scala.concurrent.Future

class ApplicationControllerSpec extends AnyFreeSpec with SpecBase with ModelGenerators with BeforeAndAfterEach {

  private val fixedClock             = Clock.fixed(Instant.now, ZoneId.systemDefault)
  private val mockApplicationService = mock(classOf[ApplicationService])
  private val mockAuthConnector      = mock(classOf[AuthConnector])

  private val applicantEori       = "applicantEori"
  private val atarEnrolment       = Enrolments(
    Set(
      Enrolment("HMRC-ATAR-ORG", Seq(EnrolmentIdentifier("EORINumber", applicantEori)), "Activated")
    )
  )
  private val trader              =
    TraderDetail("eori", "name", "line1", None, None, "postcode", "GB", None, Some(true))
  private val goodsDetails        = GoodsDetails("description", None, None, None, None, None)
  private val submissionReference = "submissionReference"
  private val method              = MethodOne(None, None, None)
  private val contact             = ContactDetails("name", "email", None, None, None)

  private val app: api.Application =
    applicationBuilder
      .overrides(
        bind[Clock].toInstance(fixedClock),
        bind[ApplicationService].toInstance(mockApplicationService),
        bind[AuthConnector].toInstance(mockAuthConnector)
      )
      .build()

  override def beforeEach(): Unit = {
    reset(mockApplicationService)
    reset(mockApplicationRepository)
    reset(mockAuthConnector)
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
        attachments = Nil,
        whatIsYourRole = WhatIsYourRole.EmployeeOrg,
        letterOfAuthority = None
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
        whatIsYourRoleResponse = Some(WhatIsYourRole.EmployeeOrg),
        letterOfAuthority = None,
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
