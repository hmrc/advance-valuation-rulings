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
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks
import play.api.Application
import play.api.inject.bind
import play.api.libs.json.Json
import play.api.test.FakeRequest
import play.api.test.Helpers._
import uk.gov.hmrc.advancevaluationrulings.base.SpecBase
import uk.gov.hmrc.advancevaluationrulings.generators.ModelGenerators
import uk.gov.hmrc.advancevaluationrulings.services.TraderDetailsService
import uk.gov.hmrc.auth.core._
import uk.gov.hmrc.auth.core.retrieve.~

import scala.concurrent.Future

class TraderDetailsControllerSpec
    extends AnyFreeSpec
    with SpecBase
    with ModelGenerators
    with BeforeAndAfterEach
    with ScalaCheckPropertyChecks {

  private val mockAuthConnector        = mock(classOf[AuthConnector])
  private val mockTraderDetailsService = mock(classOf[TraderDetailsService])

  private val ackRef        = "ackRef"
  private val eoriNumber    = "applicantEori"
  private val atarEnrolment =
    Enrolments(
      Set(
        Enrolment(
          "HMRC-ATAR-ORG",
          Seq(EnrolmentIdentifier("EORINumber", eoriNumber)),
          "Activated"
        )
      )
    )

  private val app: Application =
    applicationBuilder
      .overrides(
        bind[AuthConnector].toInstance(mockAuthConnector),
        bind[TraderDetailsService].toInstance(mockTraderDetailsService)
      )
      .build()

  override def beforeEach(): Unit = {
    super.beforeEach()
    reset(mockTraderDetailsService)
    reset(mockAuthConnector)
  }

  ".retrieveTraderDetails" - {

    "must return trader details" in {
      forAll(traderDetailsResponseGen) { traderDetails =>
        val authResult =
          new ~(
            new ~(new ~(atarEnrolment, Some("internalId")), Some(AffinityGroup.Organisation)),
            Some(Assistant)
          )

        when(mockAuthConnector.authorise[authResult.type](any(), any())(any(), any()))
          .thenReturn(Future.successful(authResult))
        when(
          mockTraderDetailsService.getTraderDetails(
            eqTo(ackRef),
            eqTo(eoriNumber)
          )(any(), any())
        )
          .thenReturn(Future.successful(Some(traderDetails)))

        val request =
          FakeRequest(
            GET,
            routes.TraderDetailsController
              .retrieveTraderDetails(ackRef, eoriNumber)
              .url
          )

        val result = route(app, request).value

        status(result) mustEqual OK
        contentAsJson(result) mustEqual Json.toJson(traderDetails)
      }
    }

    "must return not found" in {
      val authResult =
        new ~(
          new ~(new ~(atarEnrolment, Some("internalId")), Some(AffinityGroup.Organisation)),
          Some(Assistant)
        )

      when(mockAuthConnector.authorise[authResult.type](any(), any())(any(), any()))
        .thenReturn(Future.successful(authResult))
      when(
        mockTraderDetailsService.getTraderDetails(
          eqTo(ackRef),
          eqTo(eoriNumber)
        )(any(), any())
      )
        .thenReturn(Future.successful(None))

      val request =
        FakeRequest(
          GET,
          routes.TraderDetailsController
            .retrieveTraderDetails(ackRef, eoriNumber)
            .url
        )

      val result = route(app, request).value

      status(result) mustEqual NOT_FOUND
    }
  }
}
