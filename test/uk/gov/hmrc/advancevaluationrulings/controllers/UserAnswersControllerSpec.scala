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

import org.mockito.ArgumentMatchers.any
import org.mockito.ArgumentMatchersSugar.eqTo
import org.mockito.MockitoSugar
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers
import org.scalatest.{BeforeAndAfterEach, OptionValues}
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.Json
import play.api.test.FakeRequest
import play.api.test.Helpers._
import uk.gov.hmrc.advancevaluationrulings.models.{Done, DraftId, UserAnswers}
import uk.gov.hmrc.advancevaluationrulings.repositories.UserAnswersRepository
import uk.gov.hmrc.auth.core.retrieve.~
import uk.gov.hmrc.auth.core._
import uk.gov.hmrc.http.HeaderNames

import java.time.temporal.ChronoUnit
import java.time.{Clock, Instant, ZoneId}
import scala.concurrent.Future

class UserAnswersControllerSpec extends AnyFreeSpec with Matchers with MockitoSugar with OptionValues with ScalaFutures with BeforeAndAfterEach {

  private val mockRepo = mock[UserAnswersRepository]
  private val mockAuthConnector = mock[AuthConnector]

  private val instant = Instant.now.truncatedTo(ChronoUnit.MILLIS)
  private val stubClock = Clock.fixed(instant, ZoneId.systemDefault)
  private val userId = "foo"
  private val draftId = DraftId(0)
  private val userAnswers = UserAnswers(userId, draftId, Json.obj(), Instant.now(stubClock))
  private val applicantEori = "applicantEori"
  private val atarEnrolment = Enrolments(Set(Enrolment("HMRC-ATAR-ORG", Seq(EnrolmentIdentifier("EORINumber", applicantEori)), "Activated")))

  override def beforeEach(): Unit = {
    reset(mockRepo, mockAuthConnector)
    super.beforeEach()
  }

  private val app =
    new GuiceApplicationBuilder()
      .overrides(
        bind[UserAnswersRepository].toInstance(mockRepo),
        bind[AuthConnector].toInstance(mockAuthConnector)
      )
      .build()

  ".get" - {

    "must return data when it exists for this userId and draftId" in {

      when(mockRepo.get(eqTo(userId), eqTo(draftId))) thenReturn Future.successful(Some(userAnswers))
      when(mockAuthConnector.authorise[Enrolments ~ Option[String] ~ Option[AffinityGroup] ~ Option[CredentialRole]](any(), any())(any(), any()))
        .thenReturn(Future.successful(new~(new~(new~(atarEnrolment, Some(userId)), Some(AffinityGroup.Organisation)), Some(Assistant))))

      val request =
        FakeRequest(GET, routes.UserAnswersController.get(draftId).url)
          .withHeaders(HeaderNames.xSessionId -> userId)

      val result = route(app, request).value

      status(result) mustEqual OK
      contentAsJson(result) mustEqual Json.toJson(userAnswers)
    }

    "must return Not Found when data cannot be found for this user id and draft id" in {

      when(mockRepo.get(eqTo(userId), eqTo(draftId))) thenReturn Future.successful(None)
      when(mockAuthConnector.authorise[Enrolments ~ Option[String] ~ Option[AffinityGroup] ~ Option[CredentialRole]](any(), any())(any(), any()))
        .thenReturn(Future.successful(new~(new~(new~(atarEnrolment, Some(userId)), Some(AffinityGroup.Organisation)), Some(Assistant))))

      val request =
        FakeRequest(GET, routes.UserAnswersController.get(draftId).url)
          .withHeaders(HeaderNames.xSessionId -> userId)

      val result = route(app, request).value

      status(result) mustEqual NOT_FOUND
    }
  }

  ".set" - {

    "must return No Content when the data is successfully saved" in {

      when(mockRepo.set(any())) thenReturn Future.successful(Done)
      when(mockAuthConnector.authorise[Enrolments ~ Option[String] ~ Option[AffinityGroup] ~ Option[CredentialRole]](any(), any())(any(), any()))
        .thenReturn(Future.successful(new~(new~(new~(atarEnrolment, Some(userId)), Some(AffinityGroup.Organisation)), Some(Assistant))))

      val request =
        FakeRequest(POST, routes.UserAnswersController.set().url)
          .withHeaders(
            "Content-Type" -> "application/json"
          )
          .withBody(Json.toJson(userAnswers).toString)

      val result = route(app, request).value

      status(result) mustEqual NO_CONTENT
      verify(mockRepo, times(1)).set(eqTo(userAnswers))
    }

    "must return BadRequest when the data cannot be parsed as UserAnswers" in {

      when(mockAuthConnector.authorise[Enrolments ~ Option[String] ~ Option[AffinityGroup] ~ Option[CredentialRole]](any(), any())(any(), any()))
        .thenReturn(Future.successful(new~(new~(new~(atarEnrolment, Some(userId)), Some(AffinityGroup.Organisation)), Some(Assistant))))

      val request =
        FakeRequest(POST, routes.UserAnswersController.set().url)
          .withHeaders(
            "Content-Type" -> "application/json"
          )
          .withBody(Json.obj("foo" -> "bar").toString)

      val result = route(app, request).value

      status(result) mustEqual BAD_REQUEST
    }
  }

  ".keepAlive" - {

    "must return NoContent and keep the data alive" in {

      when(mockRepo.keepAlive(any(), any())) thenReturn Future.successful(Done)
      when(mockAuthConnector.authorise[Enrolments ~ Option[String] ~ Option[AffinityGroup] ~ Option[CredentialRole]](any(), any())(any(), any()))
        .thenReturn(Future.successful(new~(new~(new~(atarEnrolment, Some(userId)), Some(AffinityGroup.Organisation)), Some(Assistant))))

      val request =
        FakeRequest(POST, routes.UserAnswersController.keepAlive(draftId).url)

      val result = route(app, request).value

      status(result) mustEqual NO_CONTENT
      verify(mockRepo, times(1)).keepAlive(eqTo(userId), eqTo(draftId))
    }
  }

  ".keepAlive" - {

    "must return NoContent and clear the data" in {

      when(mockRepo.clear(any(), any())) thenReturn Future.successful(Done)
      when(mockAuthConnector.authorise[Enrolments ~ Option[String] ~ Option[AffinityGroup] ~ Option[CredentialRole]](any(), any())(any(), any()))
        .thenReturn(Future.successful(new~(new~(new~(atarEnrolment, Some(userId)), Some(AffinityGroup.Organisation)), Some(Assistant))))

      val request =
        FakeRequest(DELETE, routes.UserAnswersController.clear(draftId).url)

      val result = route(app, request).value

      status(result) mustEqual NO_CONTENT
      verify(mockRepo, times(1)).clear(eqTo(userId), eqTo(draftId))
    }
  }
}
