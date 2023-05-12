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

import uk.gov.hmrc.advancevaluationrulings.config.AppConfig
import uk.gov.hmrc.advancevaluationrulings.connectors.ETMPConnector
import uk.gov.hmrc.advancevaluationrulings.models.Done
import uk.gov.hmrc.advancevaluationrulings.models.common.{AcknowledgementReference, EoriNumber}
import uk.gov.hmrc.advancevaluationrulings.models.etmp.{ETMPSubscriptionDisplayResponse, Query, ResponseDetail, SubscriptionDisplayResponse}
import uk.gov.hmrc.advancevaluationrulings.models.etmp.Regime.CDS
import uk.gov.hmrc.advancevaluationrulings.models.traderdetails.TraderDetailsResponse
import uk.gov.hmrc.advancevaluationrulings.repositories.TraderDetailsRepository
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

  private val mockConnector  = mock[ETMPConnector]
  private val mockRepository = mock[TraderDetailsRepository]
  private val mockAppConfig  = mock[AppConfig]
  private val ackRef         = AcknowledgementReference("achRef")

  implicit val hc: HeaderCarrier = HeaderCarrier()

  override def beforeEach(): Unit = {
    super.beforeEach()
    reset(mockConnector, mockRepository)
  }

  private lazy val service = new TraderDetailsService(mockConnector, mockRepository, mockAppConfig)

  ".getTraderDetails" - {
    val responseDetail = responseDetailGen.sample.get
    val eoriNumber     = EoriNumber(responseDetail.EORINo)
    val etmpRequest    = Query(CDS, ackRef.value, EORI = Option(responseDetail.EORINo))
    val etmpResponse   = ETMPSubscriptionDisplayResponse(SubscriptionDisplayResponse(responseDetail))

    val traderDetails = TraderDetailsResponse(responseDetail)

    "when caching is enabled" - {
      "when there is a cached value, must return the trader details from the connector call and save data to cache" in {
        when(mockRepository.get(any())).thenReturn(Future.successful(None))
        when(mockConnector.getSubscriptionDetails(eqTo(etmpRequest))(any()))
          .thenReturn(Future.successful(etmpResponse))
        when(mockRepository.set(any())).thenReturn(Future.successful(Done))
        when(mockAppConfig.traderDetailsCacheEnabled).thenReturn(true)

        val result = service.getTraderDetails(ackRef, eoriNumber).futureValue

        result mustEqual traderDetails
        verify(mockRepository, times(1)).set(eqTo(traderDetails))
      }

      "when the cache is empty, must return the trader details from the cache" in {
        val traderDetails = TraderDetailsResponse(responseDetailGen.sample.get)

        when(mockRepository.get(eqTo(traderDetails.EORINo))).thenReturn(Future.successful(Some(traderDetails)))
        when(mockAppConfig.traderDetailsCacheEnabled).thenReturn(true)

        val result = service.getTraderDetails(ackRef, EoriNumber(traderDetails.EORINo)).futureValue
        verify(mockConnector, never).getSubscriptionDetails(any())(any())
        verify(mockRepository, never).set(any())

        result mustEqual traderDetails
      }
    }

    "when caching is disabled" - {
      "when the cache is empty, must return the trader details from the cache" in {
        when(mockAppConfig.traderDetailsCacheEnabled).thenReturn(false)
        when(mockConnector.getSubscriptionDetails(eqTo(etmpRequest))(any()))
          .thenReturn(Future.successful(etmpResponse))
        when(mockRepository.set(any())).thenReturn(Future.successful(Done))

        val result = service.getTraderDetails(ackRef, EoriNumber(traderDetails.EORINo)).futureValue
        verify(mockRepository, never).get(any())

        result mustEqual traderDetails
      }
    }
  }

}
