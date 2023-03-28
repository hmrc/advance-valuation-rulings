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
import org.scalatest.OptionValues
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.Json
import play.api.test.FakeRequest
import play.api.test.Helpers._
import uk.gov.hmrc.advancevaluationrulings.models.application.{Application, ApplicationId, ApplicationRequest, ApplicationSubmissionResponse, ApplicationSummaryResponse, EORIDetails}

import java.time.{Clock, Instant, ZoneId}

class ApplicationControllerSpec extends AnyFreeSpec with Matchers with OptionValues with ModelGenerators {

  private val fixedClock = Clock.fixed(Instant.now, ZoneId.systemDefault)
  private val app = GuiceApplicationBuilder().overrides(bind[Clock].toInstance(fixedClock)).build()

  ".submit" - {

    "must return an application submission response" in {

      val eoriDetails = EORIDetails("eori", "name", "line1", "line2", "line3", "postcode", "country")
      val applicationRequest = ApplicationRequest(eoriDetails)

      val request =
        FakeRequest(POST, routes.ApplicationController.submit.url)
          .withJsonBody(Json.toJson(applicationRequest))

      val result = route(app, request).value

      status(result) mustEqual OK
      contentAsJson(result) mustEqual Json.toJson(ApplicationSubmissionResponse(ApplicationId(1)))
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

    "must return an application" in {

      val applicationId = applicationIdGen.sample.value
      val expectedApplication = Application(applicationId, Instant.now(fixedClock), Instant.now(fixedClock))

      val request = FakeRequest(GET, routes.ApplicationController.get(applicationId).url)
      val result = route(app, request).value

      status(result) mustEqual OK
      contentAsJson(result) mustEqual Json.toJson(expectedApplication)
    }
  }
}
