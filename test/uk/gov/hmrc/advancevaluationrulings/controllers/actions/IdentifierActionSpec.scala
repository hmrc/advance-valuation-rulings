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

package uk.gov.hmrc.advancevaluationrulings.controllers.actions

import org.mockito.ArgumentMatchers.any
import org.mockito.MockitoSugar
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.mvc.Results.Ok
import play.api.mvc.{Action, AnyContent, BodyParsers}
import play.api.test.FakeRequest
import play.api.test.Helpers._
import uk.gov.hmrc.auth.core._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class IdentifierActionSpec extends AnyFreeSpec with Matchers with MockitoSugar {

  class Harness(identify: IdentifierAction) {
    def onPageLoad(): Action[AnyContent] = identify {
      request =>
        Ok(request.eori)
    }
  }

  private val app = new GuiceApplicationBuilder().build()
  private val bodyParsers = app.injector.instanceOf[BodyParsers.Default]

  private val mockAuthConnector: AuthConnector = mock[AuthConnector]

  "Identifier action" - {

    "when the user has the correct enrolment" - {

      "must execute the request" in {

        val atarEnrolment = Enrolments(Set(Enrolment("HMRC-ATAR-ORG", Seq(EnrolmentIdentifier("EORINumber", "eori")), "Activated")))

        when(mockAuthConnector.authorise[Enrolments](any(), any())(any(), any())) thenReturn Future.successful(atarEnrolment)

        val identifierAction = new IdentifierAction(mockAuthConnector, bodyParsers)
        val controller = new Harness(identifierAction)
        val result = controller.onPageLoad()(FakeRequest())

        status(result) mustBe OK
        contentAsString(result) mustEqual "eori"
      }
    }

    "when the user does not have the correct enrolment" - {

      "must return Unauthorized" in {

        val otherEnrolment = Enrolments(Set(Enrolment("FOO", Seq(EnrolmentIdentifier("EORINumber", "eori")), "Activated")))

        when(mockAuthConnector.authorise[Enrolments](any(), any())(any(), any())) thenReturn Future.successful(otherEnrolment)

        val identifierAction = new IdentifierAction(mockAuthConnector, bodyParsers)
        val controller = new Harness(identifierAction)
        val result = controller.onPageLoad()(FakeRequest())

        status(result) mustBe UNAUTHORIZED
      }
    }

    "when the user has no enrolments" - {

      "must return Unauthorized" in {

        when(mockAuthConnector.authorise[Enrolments](any(), any())(any(), any())) thenReturn Future.successful(Enrolments(Set.empty))

        val identifierAction = new IdentifierAction(mockAuthConnector, bodyParsers)
        val controller = new Harness(identifierAction)
        val result = controller.onPageLoad()(FakeRequest())

        status(result) mustBe UNAUTHORIZED
      }
    }
  }
}
