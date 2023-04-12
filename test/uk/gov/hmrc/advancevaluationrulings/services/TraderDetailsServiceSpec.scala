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

package uk.gov.hmrc.advancevaluationrulings.services

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

import play.api.inject
import play.api.inject.guice.GuiceApplicationBuilder
import uk.gov.hmrc.advancevaluationrulings.connectors.ETMPConnector
import uk.gov.hmrc.advancevaluationrulings.models.common.{AcknowledgementReference, EoriNumber}
import uk.gov.hmrc.advancevaluationrulings.models.etmp.{ETMPSubscriptionDisplayResponse, Query, SubscriptionDisplayResponse}
import uk.gov.hmrc.advancevaluationrulings.models.etmp.Regime.CDS
import uk.gov.hmrc.http.HeaderCarrier

import generators.ModelGenerators
import org.mockito.ArgumentMatchers.any
import org.mockito.ArgumentMatchersSugar.eqTo
import org.mockito.MockitoSugar
import org.scalatest.{BeforeAndAfterEach, OptionValues}
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks

class TraderDetailsServiceSpec
    extends AnyFreeSpec
    with Matchers
    with ScalaFutures
    with OptionValues
    with MockitoSugar
    with BeforeAndAfterEach
    with ModelGenerators
    with ScalaCheckPropertyChecks {

  private val mockConnector = mock[ETMPConnector]
  private val ackRef        = AcknowledgementReference("achRef")

  implicit val hc: HeaderCarrier = HeaderCarrier()

  override def beforeEach(): Unit = {
    super.beforeEach()
    reset(mockConnector)
  }

  private val app = GuiceApplicationBuilder()
    .overrides(inject.bind[ETMPConnector].toInstance(mockConnector))
    .build()

  private lazy val service = app.injector.instanceOf[TraderDetailsService]

  ".getTraderDetails" - {

    "must get trader details" - {

      val consentToDisclosureOfPersonalDataScenarios = Table(
        ("stubValue", "expected"),
        (None, false),
        (Option("1"), true),
        (Option("0"), false)
      )

      forAll(consentToDisclosureOfPersonalDataScenarios) {
        (stubValue, expected) =>
          s"when consentToDisclosureOfPersonalData is $stubValue" in {

            forAll(responseDetailGen) {
              responseDetail =>
                val eoriNumber   = EoriNumber(responseDetail.EORINo)
                val etmpRequest  = Query(CDS, ackRef.value, EORI = Option(eoriNumber.value))
                val etmpResponse = ETMPSubscriptionDisplayResponse(
                  SubscriptionDisplayResponse(
                    responseDetail.copy(consentToDisclosureOfPersonalData = stubValue)
                  )
                )

                when(mockConnector.getSubscriptionDetails(eqTo(etmpRequest))(any()))
                  .thenReturn(Future.successful(etmpResponse))

                val result = service.getTraderDetails(ackRef, eoriNumber).futureValue

                result.EORINo mustBe responseDetail.EORINo
                result.CDSFullName mustBe responseDetail.CDSFullName
                result.CDSEstablishmentAddress mustBe responseDetail.CDSEstablishmentAddress
                result.contactInformation mustBe responseDetail.contactInformation
                result.consentToDisclosureOfPersonalData mustBe expected
            }
          }
      }
    }
  }
}
