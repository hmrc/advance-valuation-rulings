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

import generators.ModelGenerators
import org.mockito.ArgumentMatchers.any
import org.mockito.ArgumentMatchersSugar.eqTo
import org.mockito.{Mockito, MockitoSugar}
import org.scalatest.{BeforeAndAfterEach, OptionValues}
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.Json
import play.api.test.FakeRequest
import play.api.test.Helpers._
import uk.gov.hmrc.advancevaluationrulings.models.application._
import uk.gov.hmrc.advancevaluationrulings.services.ApplicationService
import uk.gov.hmrc.auth.core.{AuthConnector, Enrolment, EnrolmentIdentifier, Enrolments}

import java.time.{Clock, Instant, ZoneId}
import scala.concurrent.Future

class ApplicationControllerSpec extends AnyFreeSpec with Matchers with OptionValues with ModelGenerators with MockitoSugar with BeforeAndAfterEach {

  private val fixedClock = Clock.fixed(Instant.now, ZoneId.systemDefault)
  private val mockApplicationService = mock[ApplicationService]
  private val mockAuthConnector = mock[AuthConnector]

  private val applicantEori = "applicantEori"
  private val atarEnrolment = Enrolments(Set(Enrolment("HMRC-ATAR-ORG", Seq(EnrolmentIdentifier("EORINumber", applicantEori)), "Activated")))
  private val trader = TraderDetail("eori", "name", "line1", None, None, "postcode", "GB", None)
  private val goodsDetails = GoodsDetails("name", "description", None, None, None)
  private val method = MethodOne(None, None, None)
  private val contact = ContactDetails("name", "email", None)

  private val app =
    GuiceApplicationBuilder()
      .overrides(
        bind[Clock].toInstance(fixedClock),
        bind[ApplicationService].toInstance(mockApplicationService),
        bind[AuthConnector].toInstance(mockAuthConnector)
      ).build()

  override def beforeEach(): Unit = {
    Mockito.reset(mockApplicationService)
    super.beforeEach()
  }

  ".submit" - {

    "must save the application and return an application submission response" in {

      val id = 123L

      when(mockAuthConnector.authorise[Enrolments](any(), any())(any(), any())) thenReturn Future.successful(atarEnrolment)
      when(mockApplicationService.save(any(), any())) thenReturn Future.successful(ApplicationId(id))

      val applicationRequest = ApplicationRequest(
        trader = trader,
        agent = None,
        contact = contact,
        goodsDetails = goodsDetails,
        requestedMethod = method,
        attachments = Nil,
      )

      val request =
        FakeRequest(POST, routes.ApplicationController.submit.url)
          .withJsonBody(Json.toJson(applicationRequest))

      val result = route(app, request).value

      status(result) mustEqual OK
      contentAsJson(result) mustEqual Json.toJson(ApplicationSubmissionResponse(ApplicationId(id)))
      verify(mockApplicationService, times(1)).save(eqTo(applicantEori), eqTo(applicationRequest))
    }
  }

  ".summaries" - {

    "must return a list of application summaries" in {

      val request = FakeRequest(GET, routes.ApplicationController.summaries.url)
      val result = route(app, request).value

      status(result) mustEqual OK
      contentAsJson(result) mustEqual Json.toJson(ApplicationSummaryResponse(Nil))
    }
  }

  ".get" - {

    "must return Ok" in {

      val applicationId = applicationIdGen.sample.value

      val request = FakeRequest(GET, routes.ApplicationController.get(applicationId).url)
      val result = route(app, request).value

      status(result) mustEqual OK
    }
  }
}