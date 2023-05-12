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
import java.time.temporal.ChronoUnit

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.Json
import play.api.test.FakeRequest
import play.api.test.Helpers._
import uk.gov.hmrc.advancevaluationrulings.models.{Done, DraftId, UserAnswers}
import uk.gov.hmrc.advancevaluationrulings.repositories.UserAnswersRepository
import uk.gov.hmrc.internalauth.client._
import uk.gov.hmrc.internalauth.client.test.{BackendAuthComponentsStub, StubBehaviour}

import org.mockito.{Mockito, MockitoSugar}
import org.mockito.ArgumentMatchers.any
import org.mockito.ArgumentMatchersSugar.eqTo
import org.scalatest.{BeforeAndAfterEach, OptionValues}
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers

class UserAnswersInternalControllerSpec extends AnyFreeSpec with Matchers with MockitoSugar with OptionValues with ScalaFutures with BeforeAndAfterEach {

  private val mockRepo                  = mock[UserAnswersRepository]
  private val mockStubBehaviour         = mock[StubBehaviour]
  private val stubBackendAuthComponents =
    BackendAuthComponentsStub(mockStubBehaviour)(stubControllerComponents(), implicitly)

  private val app = GuiceApplicationBuilder()
    .overrides(
      bind[BackendAuthComponents].toInstance(stubBackendAuthComponents),
      bind[UserAnswersRepository].toInstance(mockRepo)
    )
    .build()

  override def beforeEach(): Unit = {
    Mockito.reset(mockRepo)
    Mockito.reset(mockStubBehaviour)
    super.beforeEach()
  }

  private val writePredicate = Predicate.Permission(
    Resource(ResourceType("advance-valuation-rulings"), ResourceLocation("user-answers")),
    IAAction("WRITE")
  )
  private val readPredicate  = Predicate.Permission(
    Resource(ResourceType("advance-valuation-rulings"), ResourceLocation("user-answers")),
    IAAction("READ")
  )

  private val instant     = Instant.now.truncatedTo(ChronoUnit.MILLIS)
  private val stubClock   = Clock.fixed(instant, ZoneId.systemDefault)
  private val userId      = "foo"
  private val draftId     = DraftId(0)
  private val userAnswers = UserAnswers(userId, draftId, Json.obj(), Instant.now(stubClock))

  ".get" - {

    "must return a draft when one exists for this draft id" in {

      when(mockRepo.get(draftId)).thenReturn(Future.successful(Some(userAnswers)))
      when(mockStubBehaviour.stubAuth[Unit](any(), any())).thenReturn(Future.unit)

      val request = FakeRequest(GET, routes.UserAnswersInternalController.get(draftId).url)
        .withHeaders(AUTHORIZATION -> "Some auth token")

      val result = route(app, request).value

      status(result) mustEqual OK
      contentAsJson(result) mustEqual Json.toJson(userAnswers)

      verify(mockStubBehaviour).stubAuth(Some(readPredicate), Retrieval.EmptyRetrieval)
    }

    "must return not found when no record exists for this draft id" in {

      when(mockRepo.get(draftId)).thenReturn(Future.successful(None))
      when(mockStubBehaviour.stubAuth[Unit](any(), any())).thenReturn(Future.unit)

      val request = FakeRequest(GET, routes.UserAnswersInternalController.get(draftId).url)
        .withHeaders(AUTHORIZATION -> "Some auth token")

      val result = route(app, request).value

      status(result) mustEqual NOT_FOUND

      verify(mockStubBehaviour).stubAuth(Some(readPredicate), Retrieval.EmptyRetrieval)
    }

    "must fail for an unauthenticated call" in {

      val request =
        FakeRequest(GET, routes.UserAnswersInternalController.get(draftId).url) // No auth token

      route(app, request).value.failed.futureValue
    }

    "must fail for an unauthorised call" in {

      when(mockStubBehaviour.stubAuth[Unit](any(), any()))
        .thenReturn(Future.failed(new RuntimeException()))

      val request = FakeRequest(GET, routes.UserAnswersInternalController.get(draftId).url)
        .withHeaders(AUTHORIZATION -> "Some auth token")

      route(app, request).value.failed.futureValue
    }
  }

  ".set" - {

    "must save the given answers and return NO_CONTENT" in {

      when(mockRepo.set(any())).thenReturn(Future.successful(Done))
      when(mockStubBehaviour.stubAuth[Unit](any(), any())).thenReturn(Future.unit)

      val request = FakeRequest(POST, routes.UserAnswersInternalController.set().url)
        .withHeaders(AUTHORIZATION -> "Some auth token")
        .withJsonBody(Json.toJson(userAnswers))

      val result = route(app, request).value

      status(result) mustEqual NO_CONTENT

      verify(mockStubBehaviour).stubAuth(Some(writePredicate), Retrieval.EmptyRetrieval)
      verify(mockRepo, times(1)).set(eqTo(userAnswers))
    }

    "must return BAD_REQUEST when invalid data is received" in {

      val request = FakeRequest(POST, routes.UserAnswersInternalController.set().url)
        .withHeaders(AUTHORIZATION -> "Some auth token")
        .withJsonBody(Json.obj("foo" -> "bar"))

      val result = route(app, request).value

      status(result) mustEqual BAD_REQUEST
    }

    "must fail for an unauthenticated call" in {

      val request = FakeRequest(POST, routes.UserAnswersInternalController.set().url)
        .withJsonBody(Json.toJson(userAnswers)) // No auth token

      route(app, request).value.failed.futureValue
    }

    "must fail for an unauthorised call" in {

      when(mockStubBehaviour.stubAuth[Unit](any(), any()))
        .thenReturn(Future.failed(new RuntimeException()))

      val request = FakeRequest(POST, routes.UserAnswersInternalController.set().url)
        .withHeaders(AUTHORIZATION -> "Some auth token")
        .withJsonBody(Json.toJson(userAnswers))

      route(app, request).value.failed.futureValue
    }
  }
}
