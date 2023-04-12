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
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers
import org.scalatest.{BeforeAndAfterEach, OptionValues}
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.Json
import play.api.test.FakeRequest
import play.api.test.Helpers._
import uk.gov.hmrc.advancevaluationrulings.connectors.ETMPConnector
import uk.gov.hmrc.advancevaluationrulings.models.etmp.Query
import uk.gov.hmrc.advancevaluationrulings.models.etmp.Regime.CDS
import uk.gov.hmrc.advancevaluationrulings.models.traderdetails.TraderDetailsResponse
import uk.gov.hmrc.auth.core.retrieve.~
import uk.gov.hmrc.auth.core.{AffinityGroup, Assistant, AuthConnector, CredentialRole, Enrolment, EnrolmentIdentifier, Enrolments}

import scala.concurrent.Future

class TraderDetailsControllerSpec extends AnyFreeSpec with Matchers with OptionValues with ModelGenerators with MockitoSugar with BeforeAndAfterEach {

  private val mockConnector = mock[ETMPConnector]
  private val mockAuthConnector = mock[AuthConnector]

  private val applicantEori = "applicantEori"
  private val atarEnrolment = Enrolments(Set(Enrolment("HMRC-ATAR-ORG", Seq(EnrolmentIdentifier("EORINumber", applicantEori)), "Activated")))

  private val app =
    GuiceApplicationBuilder()
      .overrides(
        bind[ETMPConnector].toInstance(mockConnector),
        bind[AuthConnector].toInstance(mockAuthConnector)
      ).build()

  override def beforeEach(): Unit = {
    Mockito.reset(mockConnector)
    Mockito.reset(mockAuthConnector)
    super.beforeEach()
  }

  ".retrieveTraderDetails" - {

    "must return trader details" in {

      val etmpRequest = Query(CDS, "foo", EORI = Some("eori"))
      val etmpResponse = ETMPSubscriptionDisplayResponseGen.sample.value
      val expectedResult = TraderDetailsResponse(
        etmpResponse.subscriptionDisplayResponse.responseDetail.EORINo,
        etmpResponse.subscriptionDisplayResponse.responseDetail.CDSFullName,
        etmpResponse.subscriptionDisplayResponse.responseDetail.CDSEstablishmentAddress,
        etmpResponse.subscriptionDisplayResponse.responseDetail.consentToDisclosureOfPersonalData
          .exists(_.equalsIgnoreCase("1")),
        etmpResponse.subscriptionDisplayResponse.responseDetail.contactInformation
      )

      when(mockAuthConnector.authorise[Enrolments ~ Option[String] ~ Option[AffinityGroup] ~ Option[CredentialRole]](any(), any())(any(), any()))
        .thenReturn(Future.successful(new ~(new ~(new ~(atarEnrolment, Some("internalId")), Some(AffinityGroup.Organisation)), Some(Assistant))))
      when(mockConnector.getSubscriptionDetails(eqTo(etmpRequest))(any())).thenReturn(Future.successful(etmpResponse))

      val request =
        FakeRequest(GET, routes.TraderDetailsController.retrieveTraderDetails("foo", "eori").url)

      val result = route(app, request).value

      status(result) mustEqual OK
      contentAsJson(result) mustEqual Json.toJson(expectedResult)
    }
  }
}
