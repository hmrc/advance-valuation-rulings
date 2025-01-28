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
import org.mockito.Mockito.*
import org.scalatest.BeforeAndAfterEach
import play.api.Application
import play.api.inject.bind
import play.api.libs.json.{JsValue, Json}
import play.api.mvc.Result
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import uk.gov.hmrc.advancevaluationrulings.base.SpecBase
import uk.gov.hmrc.advancevaluationrulings.models.dms.{NotificationRequest, SubmissionItemStatus}
import uk.gov.hmrc.http.UpstreamErrorResponse
import uk.gov.hmrc.internalauth.client.*
import uk.gov.hmrc.internalauth.client.Predicate.Permission
import uk.gov.hmrc.internalauth.client.test.{BackendAuthComponentsStub, StubBehaviour}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class DmsSubmissionCallbackControllerSpec extends SpecBase with BeforeAndAfterEach {

  override def beforeEach(): Unit = {
    super.beforeEach()
    reset(mockStubBehaviour)
  }

  private val mockStubBehaviour: StubBehaviour                 = mock(classOf[StubBehaviour])
  private val stubBackendAuthComponents: BackendAuthComponents =
    BackendAuthComponentsStub(mockStubBehaviour)(stubControllerComponents(), implicitly)

  private val app: Application = applicationBuilder
    .overrides(
      bind[BackendAuthComponents].toInstance(stubBackendAuthComponents)
    )
    .build()

  private val predicate: Permission = Permission(
    Resource(ResourceType("advance-valuation-rulings"), ResourceLocation("dms/callback")),
    IAAction("WRITE")
  )

  private val notification: NotificationRequest = NotificationRequest(
    id = "id",
    status = SubmissionItemStatus.Processed,
    failureReason = None
  )

  "callback" - {

    SubmissionItemStatus.values.foreach { notificationsStatus =>
      s"must return OK when a valid request is received with status $notificationsStatus" in {

        when(mockStubBehaviour.stubAuth[Unit](any(), any())).thenReturn(Future.unit)

        val request: FakeRequest[JsValue] = FakeRequest(POST, routes.DmsSubmissionCallbackController.callback.url)
          .withHeaders(AUTHORIZATION -> "Some auth token")
          .withBody(Json.toJson(notification.copy(status = notificationsStatus)))

        val result: Future[Result] = route(app, request).value

        status(result) mustBe OK

        verify(mockStubBehaviour).stubAuth(Some(predicate), Retrieval.EmptyRetrieval)
      }
    }

    "must return BAD_REQUEST when an invalid request is received" in {

      when(mockStubBehaviour.stubAuth[Unit](any(), any())).thenReturn(Future.unit)

      val request: FakeRequest[JsValue] = FakeRequest(POST, routes.DmsSubmissionCallbackController.callback.url)
        .withHeaders(AUTHORIZATION -> "Some auth token")
        .withBody(Json.obj())

      val result: Future[Result] = route(app, request).value

      status(result) mustBe BAD_REQUEST
    }

    "must fail for an unauthenticated user i.e. no Authorization header" in {

      val request: FakeRequest[JsValue] = FakeRequest(POST, routes.DmsSubmissionCallbackController.callback.url)
        .withBody(Json.toJson(notification))

      val result: Throwable = route(app, request).value.failed.futureValue

      result.getMessage mustBe "Unauthorized"
      result mustBe an[UpstreamErrorResponse]
    }

    "must fail when the user is not authorised" in {

      when(mockStubBehaviour.stubAuth[Unit](any(), any()))
        .thenReturn(Future.failed(new RuntimeException()))

      val request: FakeRequest[JsValue] = FakeRequest(POST, routes.DmsSubmissionCallbackController.callback.url)
        .withHeaders(AUTHORIZATION -> "Some auth token")
        .withBody(Json.toJson(notification))

      val result: Throwable = route(app, request).value.failed.futureValue

      result mustBe a[RuntimeException]
    }

    "must return OK when a valid request is received but notification is FAILED" in {

      when(mockStubBehaviour.stubAuth[Unit](any(), any())).thenReturn(Future.unit)

      val request: FakeRequest[JsValue] = FakeRequest(POST, routes.DmsSubmissionCallbackController.callback.url)
        .withHeaders(AUTHORIZATION -> "Some auth token")
        .withBody(Json.toJson(notification.copy(status = SubmissionItemStatus.Failed)))

      val result: Future[Result] = route(app, request).value

      status(result) mustBe OK

      verify(mockStubBehaviour).stubAuth(Some(predicate), Retrieval.EmptyRetrieval)
    }

    Seq(
      UpstreamErrorResponse("Unauthorized", UNAUTHORIZED),
      UpstreamErrorResponse("Forbidden", FORBIDDEN)
    ).foreach { response =>
      s"must fail if auth.authorizedAction fails and returns ${response.statusCode}" in {

        when(mockStubBehaviour.stubAuth[Unit](any(), any()))
          .thenReturn(Future.failed(response))

        val request: FakeRequest[JsValue] = FakeRequest(POST, routes.DmsSubmissionCallbackController.callback.url)
          .withHeaders(AUTHORIZATION -> "Some auth token")
          .withBody(Json.toJson(notification))

        val result: Throwable = route(app, request).value.failed.futureValue

        result.getMessage mustBe response.message
        result mustBe an[UpstreamErrorResponse]
      }
    }
  }
}
