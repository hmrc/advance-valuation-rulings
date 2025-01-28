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

package uk.gov.hmrc.advancevaluationrulings.controllers

import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito._
import org.scalatest.BeforeAndAfterEach
import play.api.Application
import play.api.inject.bind
import play.api.libs.json.Json
import play.api.mvc.{AnyContentAsEmpty, AnyContentAsJson, Result}
import play.api.test.FakeRequest
import play.api.test.Helpers._
import uk.gov.hmrc.advancevaluationrulings.base.SpecBase
import uk.gov.hmrc.advancevaluationrulings.models.{Done, DraftId, UserAnswers}
import uk.gov.hmrc.http.UpstreamErrorResponse
import uk.gov.hmrc.internalauth.client.Predicate.Permission
import uk.gov.hmrc.internalauth.client._
import uk.gov.hmrc.internalauth.client.test.{BackendAuthComponentsStub, StubBehaviour}

import java.time.temporal.ChronoUnit
import java.time.{Clock, Instant, ZoneId}
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class UserAnswersInternalControllerSpec extends SpecBase with BeforeAndAfterEach {

  private val mockStubBehaviour: StubBehaviour                 = mock(classOf[StubBehaviour])
  private val stubBackendAuthComponents: BackendAuthComponents =
    BackendAuthComponentsStub(mockStubBehaviour)(stubControllerComponents(), implicitly)

  private val app: Application = applicationBuilder
    .overrides(
      bind[BackendAuthComponents].toInstance(stubBackendAuthComponents)
    )
    .build()

  override def beforeEach(): Unit = {
    reset(mockUserAnswersRepository)
    reset(mockStubBehaviour)
    super.beforeEach()
  }

  private val writePredicate: Permission = Permission(
    Resource(ResourceType("advance-valuation-rulings"), ResourceLocation("user-answers")),
    IAAction("WRITE")
  )
  private val readPredicate: Permission  = Permission(
    Resource(ResourceType("advance-valuation-rulings"), ResourceLocation("user-answers")),
    IAAction("READ")
  )

  private val instant: Instant         = Instant.now.truncatedTo(ChronoUnit.MILLIS)
  private val stubClock: Clock         = Clock.fixed(instant, ZoneId.systemDefault)
  private val userId: String           = "foo"
  private val draftId: DraftId         = DraftId(0)
  private val userAnswers: UserAnswers = UserAnswers(userId, draftId, Json.obj(), Instant.now(stubClock))

  ".get" - {

    "must return a draft when one exists for this draft id" in {

      when(mockUserAnswersRepository.get(draftId)).thenReturn(Future.successful(Some(userAnswers)))
      when(mockStubBehaviour.stubAuth[Unit](any(), any())).thenReturn(Future.unit)

      val request: FakeRequest[AnyContentAsEmpty.type] =
        FakeRequest(GET, routes.UserAnswersInternalController.get(draftId).url)
          .withHeaders(AUTHORIZATION -> "Some auth token")

      val result: Future[Result] = route(app, request).value

      status(result) mustBe OK
      contentAsJson(result) mustBe Json.toJson(userAnswers)

      verify(mockStubBehaviour).stubAuth(Some(readPredicate), Retrieval.EmptyRetrieval)
    }

    "must return not found when no record exists for this draft id" in {

      when(mockUserAnswersRepository.get(draftId)).thenReturn(Future.successful(None))
      when(mockStubBehaviour.stubAuth[Unit](any(), any())).thenReturn(Future.unit)

      val request: FakeRequest[AnyContentAsEmpty.type] =
        FakeRequest(GET, routes.UserAnswersInternalController.get(draftId).url)
          .withHeaders(AUTHORIZATION -> "Some auth token")

      val result: Future[Result] = route(app, request).value

      status(result) mustBe NOT_FOUND

      verify(mockStubBehaviour).stubAuth(Some(readPredicate), Retrieval.EmptyRetrieval)
    }

    "must fail for an unauthenticated call i.e. no Authorization header" in {

      val request: FakeRequest[AnyContentAsEmpty.type] =
        FakeRequest(GET, routes.UserAnswersInternalController.get(draftId).url)

      val result: Throwable = route(app, request).value.failed.futureValue

      result.getMessage mustBe "Unauthorized"
      result mustBe an[UpstreamErrorResponse]
    }

    "must fail for an unauthorised call" in {

      when(mockStubBehaviour.stubAuth[Unit](any(), any()))
        .thenReturn(Future.failed(new RuntimeException()))

      val request: FakeRequest[AnyContentAsEmpty.type] =
        FakeRequest(GET, routes.UserAnswersInternalController.get(draftId).url)
          .withHeaders(AUTHORIZATION -> "Some auth token")

      val result: Throwable = route(app, request).value.failed.futureValue

      result mustBe a[RuntimeException]
    }

    Seq(
      UpstreamErrorResponse("Unauthorized", UNAUTHORIZED),
      UpstreamErrorResponse("Forbidden", FORBIDDEN)
    ).foreach { response =>
      s"must fail if auth.authorizedAction fails and returns ${response.statusCode}" in {

        when(mockStubBehaviour.stubAuth[Unit](any(), any()))
          .thenReturn(Future.failed(response))

        val request: FakeRequest[AnyContentAsEmpty.type] =
          FakeRequest(GET, routes.UserAnswersInternalController.get(draftId).url)
            .withHeaders(AUTHORIZATION -> "Some auth token")

        val result: Throwable = route(app, request).value.failed.futureValue

        result.getMessage mustBe response.message
        result mustBe an[UpstreamErrorResponse]
      }
    }
  }

  ".set" - {

    "must save the given answers and return NO_CONTENT" in {

      when(mockUserAnswersRepository.set(any())).thenReturn(Future.successful(Done))
      when(mockStubBehaviour.stubAuth[Unit](any(), any())).thenReturn(Future.unit)

      val request: FakeRequest[AnyContentAsJson] = FakeRequest(POST, routes.UserAnswersInternalController.set().url)
        .withHeaders(AUTHORIZATION -> "Some auth token")
        .withJsonBody(Json.toJson(userAnswers))

      val result: Future[Result] = route(app, request).value

      status(result) mustBe NO_CONTENT

      verify(mockStubBehaviour).stubAuth(Some(writePredicate), Retrieval.EmptyRetrieval)
      verify(mockUserAnswersRepository, times(1)).set(userAnswers)
    }

    "must return BAD_REQUEST when invalid data is received" in {

      val request: FakeRequest[AnyContentAsJson] = FakeRequest(POST, routes.UserAnswersInternalController.set().url)
        .withHeaders(AUTHORIZATION -> "Some auth token")
        .withJsonBody(Json.obj("foo" -> "bar"))

      val result: Future[Result] = route(app, request).value

      status(result) mustBe BAD_REQUEST
    }

    "must fail for an unauthenticated call i.e. no Authorization header" in {

      val request: FakeRequest[AnyContentAsJson] = FakeRequest(POST, routes.UserAnswersInternalController.set().url)
        .withJsonBody(Json.toJson(userAnswers))

      val result: Throwable = route(app, request).value.failed.futureValue

      result.getMessage mustBe "Unauthorized"
      result mustBe an[UpstreamErrorResponse]
    }

    "must fail for an unauthorised call" in {

      when(mockStubBehaviour.stubAuth[Unit](any(), any()))
        .thenReturn(Future.failed(new RuntimeException()))

      val request: FakeRequest[AnyContentAsJson] = FakeRequest(POST, routes.UserAnswersInternalController.set().url)
        .withHeaders(AUTHORIZATION -> "Some auth token")
        .withJsonBody(Json.toJson(userAnswers))

      val result: Throwable = route(app, request).value.failed.futureValue

      result mustBe a[RuntimeException]
    }

    Seq(
      UpstreamErrorResponse("Unauthorized", UNAUTHORIZED),
      UpstreamErrorResponse("Forbidden", FORBIDDEN)
    ).foreach { response =>
      s"must fail if auth.authorizedAction fails and returns ${response.statusCode}" in {

        when(mockStubBehaviour.stubAuth[Unit](any(), any()))
          .thenReturn(Future.failed(response))

        val request: FakeRequest[AnyContentAsJson] = FakeRequest(POST, routes.UserAnswersInternalController.set().url)
          .withHeaders(AUTHORIZATION -> "Some auth token")
          .withJsonBody(Json.toJson(userAnswers))

        val result: Throwable = route(app, request).value.failed.futureValue

        result.getMessage mustBe response.message
        result mustBe an[UpstreamErrorResponse]
      }
    }
  }
}
